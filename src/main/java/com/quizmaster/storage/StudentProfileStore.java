package com.quizmaster.storage;

import com.quizmaster.model.StudentProfile;
import com.quizmaster.security.PasswordHash;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** UTF-8 CSV storage for local student profiles. */
public final class StudentProfileStore {
    private static final Object REGISTRATION_LOCK = new Object();
    private static final List<String> LEGACY_HEADER = List.of("student_id", "display_name");
    private static final List<String> HEADER = List.of("student_id", "display_name", "password_salt", "password_hash", "iterations", "must_change_password");
    private final Path file;

    public StudentProfileStore(Path dataDirectory) { this.file = dataDirectory.resolve("students.csv"); }

    public synchronized List<StudentProfile> loadAll() throws IOException {
        Map<String, AccountRecord> records = readRecords(false);
        return records.values().stream().map(AccountRecord::profile).toList();
    }

    public synchronized StudentProfile find(String id) throws IOException {
        if (id == null || id.trim().isEmpty()) return null;
        String normalized = normalizeId(id.trim());
        for (StudentProfile profile : loadAll()) {
            if (normalizeId(profile.getStudentId()).equals(normalized)) return profile;
        }
        return null;
    }

    public synchronized StudentProfile create(String displayName, String studentId) throws IOException {
        String name = displayName == null ? "" : displayName.trim();
        String id = studentId == null ? "" : studentId.trim();
        if (name.isEmpty()) throw new IllegalArgumentException("Enter your display name.");
        if (name.length() > 80 || containsControl(name)) throw new IllegalArgumentException("Display name must be 1–80 printable characters.");
        if (!validId(id)) throw new IllegalArgumentException("Student ID must be 1–32 letters, digits, dots, underscores, or hyphens.");
        Map<String,AccountRecord> records=readRecords(true);
        if(records.containsKey(normalizeId(id)))throw new IllegalArgumentException("That student ID is already registered. Sign in with its password.");
        StudentProfile profile = new StudentProfile(id, name);
        records.put(normalizeId(id),new AccountRecord(profile,"","","","false"));
        saveRecords(records);
        return profile;
    }

    /** Registers a password-protected account; an unclaimed legacy profile can only be claimed by matching its saved name. */
    public synchronized StudentProfile register(String displayName,String studentId,char[] password,char[] confirmation) throws IOException {
        String name=validateName(displayName); String id=validateId(studentId,"Student ID");
        if(password==null||confirmation==null||!java.util.Arrays.equals(password,confirmation))throw new IllegalArgumentException("The passwords do not match.");
        PasswordHash.validate(password);
        Map<String,AccountRecord> records=readRecords(true); String key=normalizeId(id); AccountRecord existing=records.get(key);
        if(existing!=null && existing.hasCredential())throw new IllegalArgumentException("That student ID is already registered. Sign in instead.");
        if(existing!=null&&!existing.profile.getDisplayName().equalsIgnoreCase(name))throw new IllegalArgumentException("That ID belongs to a legacy profile. Enter its saved display name to claim it.");
        StudentProfile profile=existing==null?new StudentProfile(id,name):existing.profile;
        String[] hash=PasswordHash.create(password);
        records.put(key,new AccountRecord(profile,hash[0],hash[1],hash[2],"false")); saveRecords(records); return profile;
    }

    /** Atomically chooses the next unused STU ID and writes the account. */
    public StudentProfile registerGenerated(String displayName,char[] password,char[] confirmation)throws IOException{
        synchronized(REGISTRATION_LOCK){Map<String,AccountRecord> records=readRecords(true);int next=1001;for(AccountRecord a:records.values()){String id=a.profile.getStudentId().toUpperCase(Locale.ROOT);if(id.matches("STU[0-9]+")){try{next=Math.max(next,Integer.parseInt(id.substring(3))+1);}catch(NumberFormatException ignored){}}}String id;do{id=String.format(Locale.ROOT,"STU%04d",next++);}while(records.containsKey(normalizeId(id)));return register(displayName,id,password,confirmation);}
    }
    /** Secures an existing passwordless legacy row; it cannot create or claim an arbitrary new ID. */
    public StudentProfile claimLegacyProfile(String displayName,String studentId,char[] password,char[] confirmation)throws IOException{
        synchronized(REGISTRATION_LOCK){String name=validateName(displayName),id=validateId(studentId,"Student ID");Map<String,AccountRecord> records=readRecords(true);AccountRecord old=records.get(normalizeId(id));if(old==null||old.hasCredential())throw new IllegalArgumentException("No passwordless legacy profile with that ID is available to claim.");if(!old.profile.getDisplayName().equalsIgnoreCase(name))throw new IllegalArgumentException("The saved display name does not match this legacy profile.");if(password==null||confirmation==null||!java.util.Arrays.equals(password,confirmation))throw new IllegalArgumentException("The passwords do not match.");PasswordHash.validate(password);String[] h=PasswordHash.create(password);records.put(normalizeId(id),new AccountRecord(old.profile,h[0],h[1],h[2],"false"));saveRecords(records);return old.profile;}
    }

    public synchronized StudentProfile authenticate(String studentId,char[] password) throws IOException {
        if(studentId==null||password==null)return null;
        AccountRecord record=readRecords(false).get(normalizeId(studentId.trim()));
        if(record==null||!record.hasCredential())return null;
        return PasswordHash.verify(password,record.salt,record.hash,record.iterations)?record.profile:null;
    }
    public synchronized void changePassword(String studentId,char[] current,char[] replacement,char[] confirmation)throws IOException{
        Map<String,AccountRecord> records=readRecords(true);String key=normalizeId(studentId);AccountRecord a=records.get(key);
        if(a==null||!a.hasCredential()||current==null||!PasswordHash.verify(current,a.salt,a.hash,a.iterations))throw new SecurityException("Current password is incorrect.");
        if(replacement==null||confirmation==null||!java.util.Arrays.equals(replacement,confirmation))throw new IllegalArgumentException("The new passwords do not match.");PasswordHash.validate(replacement);String[] h=PasswordHash.create(replacement);records.put(key,new AccountRecord(a.profile,h[0],h[1],h[2],"false"));saveRecords(records);
    }

    /** Creates a demonstration account only when its ID is absent; never resets an existing record. */
    public synchronized void ensureDemoAccount(String id,String name,char[] password) throws IOException {
        Map<String,AccountRecord> records=readRecords(true); String key=normalizeId(id);
        if(records.containsKey(key))return;
        String[] hash=PasswordHash.create(password);
        records.put(key,new AccountRecord(new StudentProfile(id,name),hash[0],hash[1],hash[2],"true")); saveRecords(records);
    }

    private void ensureFile() throws IOException {
        Files.createDirectories(file.getParent());
        if (!Files.exists(file)) Files.writeString(file, Csv.encodeRow(HEADER)+System.lineSeparator(), StandardCharsets.UTF_8);
    }

    private void saveRecords(Map<String,AccountRecord> records) throws IOException {
        ensureFile();
        List<String> source=Files.readAllLines(file,StandardCharsets.UTF_8);
        List<String> oldHeader=source.isEmpty()?List.of():Csv.parse(source.get(0));
        if(!oldHeader.equals(HEADER)&&!oldHeader.isEmpty()){
            Path backup=file.resolveSibling("students.csv.pre-account-migration.bak");
            if(!Files.exists(backup))Files.copy(file,backup);
        }
        List<String> lines = new ArrayList<>();
        lines.add(Csv.encodeRow(HEADER));
        for(AccountRecord record:records.values())lines.add(Csv.encodeRow(List.of(record.profile.getStudentId(),record.profile.getDisplayName(),record.salt,record.hash,record.iterations,record.mustChange)));
        Path temporary = file.resolveSibling("students.csv.tmp");
        Files.write(temporary, lines, StandardCharsets.UTF_8);
        try { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
        catch (IOException unsupportedAtomicMove) { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING); }
    }

    private Map<String,AccountRecord> readRecords(boolean strict) throws IOException {
        ensureFile(); List<String> lines=Files.readAllLines(file,StandardCharsets.UTF_8); Map<String,AccountRecord> records=new LinkedHashMap<>();
        if(lines.isEmpty())return records;
        List<String> header; try{header=Csv.parse(lines.get(0));}catch(IllegalArgumentException ex){throw new IOException("Student account header is malformed; data was not changed.",ex);}
        boolean legacy=header.equals(LEGACY_HEADER); if(!legacy&&!header.equals(HEADER))throw new IOException("Student account columns are not recognized; data was not changed.");
        for(int i=1;i<lines.size();i++){
            String line=lines.get(i); if(line.isBlank())continue;
            try{
                List<String> row=Csv.parse(line); if(row.size()!=(legacy?2:HEADER.size()))throw new IllegalArgumentException("incorrect column count");
                String id=validateId(row.get(0),"Student ID"), name=validateName(row.get(1));
                AccountRecord record=legacy?new AccountRecord(new StudentProfile(id,name),"","","","false"):
                        new AccountRecord(new StudentProfile(id,name),row.get(2),row.get(3),row.get(4),row.get(5));
                if(records.putIfAbsent(normalizeId(id),record)!=null)throw new IllegalArgumentException("duplicate student ID");
            }catch(IllegalArgumentException ex){if(strict)throw new IOException("Student CSV line "+(i+1)+" is invalid; it was preserved and no account change was made: "+ex.getMessage(),ex);}
        }
        return records;
    }

    private static boolean validId(String id) { return id != null && id.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,31}"); }
    private static String normalizeId(String id) { return id.toLowerCase(Locale.ROOT); }
    private static boolean containsControl(String text) { return text.chars().anyMatch(Character::isISOControl); }
    private static String validateId(String id,String label){String value=id==null?"":id.trim();if(!validId(value))throw new IllegalArgumentException(label+" must use 1–32 letters, digits, dots, underscores, or hyphens.");return value;}
    private static String validateName(String name){String value=name==null?"":name.trim();if(value.isEmpty())throw new IllegalArgumentException("Enter your display name.");if(value.length()>80||containsControl(value))throw new IllegalArgumentException("Display name must be 1–80 printable characters.");return value;}
    private static final class AccountRecord{
        final StudentProfile profile;final String salt,hash,iterations,mustChange;
        AccountRecord(StudentProfile p,String s,String h,String i,String m){profile=p;salt=s;hash=h;iterations=i;mustChange=m;}
        boolean hasCredential(){return !salt.isBlank()&&!hash.isBlank()&&!iterations.isBlank();}
        StudentProfile profile(){return profile;}
    }
}
