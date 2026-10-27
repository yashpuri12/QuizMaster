package com.quizmaster.storage;

import com.quizmaster.model.TeacherProfile;
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
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/** UTF-8 persistent teacher accounts. Credential columns contain salted hashes only. */
public final class TeacherAccountStore {
    private static final Object REGISTRATION_LOCK=new Object();
    private static final SecureRandom RANDOM=new SecureRandom();
    private static final List<String> HEADER=List.of("teacher_id","display_name","password_salt","password_hash","iterations","must_change_password");
    private final Path file;
    private final Path invitationsFile;
    public TeacherAccountStore(Path directory){file=directory.resolve("teachers.csv");invitationsFile=directory.resolve("teacher-invitations.csv");}
    public synchronized List<TeacherProfile> loadAll()throws IOException{return read(true).values().stream().map(a->a.profile).toList();}
    public synchronized TeacherProfile authenticate(String id,char[] password)throws IOException{
        if(id==null||password==null)return null;Account account=read(false).get(normalize(id));
        if(account==null||account.hash.isBlank())return null;
        return PasswordHash.verify(password,account.salt,account.hash,account.iterations)?account.profile:null;
    }
    public synchronized void changePassword(String teacherId,char[] current,char[] replacement,char[] confirmation)throws IOException{
        Map<String,Account> accounts=read(true);String key=normalize(teacherId);Account a=accounts.get(key);
        if(a==null||a.hash.isBlank()||!PasswordHash.verify(current,a.salt,a.hash,a.iterations))throw new SecurityException("Current password is incorrect.");
        if(replacement==null||confirmation==null||!java.util.Arrays.equals(replacement,confirmation))throw new IllegalArgumentException("The new passwords do not match.");PasswordHash.validate(replacement);String[] h=PasswordHash.create(replacement);accounts.put(key,new Account(a.profile,h[0],h[1],h[2],"false"));write(accounts);
    }
    public synchronized TeacherProfile register(String name,String id,char[] password,char[] confirmation)throws IOException{
        String checkedId=validId(id),checkedName=validName(name);
        if(password==null||confirmation==null||!java.util.Arrays.equals(password,confirmation))throw new IllegalArgumentException("The passwords do not match.");
        PasswordHash.validate(password);Map<String,Account> accounts=read(true);String key=normalize(checkedId);
        if(accounts.containsKey(key))throw new IllegalArgumentException("That Teacher ID is already registered.");
        String[] hash=PasswordHash.create(password);TeacherProfile profile=new TeacherProfile(checkedId,checkedName);
        accounts.put(key,new Account(profile,hash[0],hash[1],hash[2],"false"));write(accounts);return profile;
    }
    /** Issues a one-use registration code; the caller must enforce teacher authorization. */
    public synchronized String issueRegistrationCode(String ownerTeacherId)throws IOException{
        synchronized(REGISTRATION_LOCK){
            String ownerId=validId(ownerTeacherId);
            if(!read(true).containsKey(normalize(ownerId)))throw new SecurityException("Only a registered teacher can issue an invitation.");
            List<Invite> invites=readInvites();String code,hash;
            do{byte[] token=new byte[24];RANDOM.nextBytes(token);code=Base64.getUrlEncoder().withoutPadding().encodeToString(token);hash=hashCode(code);}
            while(containsInvitationHash(invites,hash));
            invites.add(new Invite(hash,ownerId,false));writeInvites(invites);return code;
        }
    }
    /** Creates the teacher account only when an authorized one-use code is supplied. */
    public TeacherProfile registerWithCode(String name,char[] password,char[] confirmation,String registrationCode)throws IOException{
        synchronized(REGISTRATION_LOCK){String checkedName=validName(name);if(password==null||confirmation==null||!java.util.Arrays.equals(password,confirmation))throw new IllegalArgumentException("The passwords do not match.");PasswordHash.validate(password);if(registrationCode==null||registrationCode.isBlank())throw new SecurityException("Enter a registration code from an authorized teacher.");List<Invite> invites=readInvites();String hash=hashCode(registrationCode.trim());int index=-1;for(int i=0;i<invites.size();i++)if(!invites.get(i).used&&MessageDigest.isEqual(invites.get(i).hash.getBytes(StandardCharsets.US_ASCII),hash.getBytes(StandardCharsets.US_ASCII))){index=i;break;}if(index<0)throw new SecurityException("This registration code is invalid or has already been used.");Map<String,Account> accounts=read(true);int next=1001;for(Account a:accounts.values()){String id=a.profile.getTeacherId().toUpperCase(Locale.ROOT);if(id.matches("TCH[0-9]+")){try{next=Math.max(next,Integer.parseInt(id.substring(3))+1);}catch(NumberFormatException ignored){}}}String id;do{id=String.format(Locale.ROOT,"TCH%04d",next++);}while(accounts.containsKey(normalize(id)));String[] credential=PasswordHash.create(password);TeacherProfile profile=new TeacherProfile(id,checkedName);accounts.put(normalize(id),new Account(profile,credential[0],credential[1],credential[2],"false"));write(accounts);Invite old=invites.get(index);invites.set(index,new Invite(old.hash,old.ownerId,true));writeInvites(invites);return profile;}
    }
    public synchronized void ensureDemoAccount(String id,String name,char[] password)throws IOException{
        Map<String,Account> accounts=read(true);String key=normalize(id);if(accounts.containsKey(key))return;
        String[] hash=PasswordHash.create(password);accounts.put(key,new Account(new TeacherProfile(id,name),hash[0],hash[1],hash[2],"true"));write(accounts);
    }
    private Map<String,Account> read(boolean strict)throws IOException{
        Files.createDirectories(file.getParent());if(!Files.exists(file))Files.writeString(file,Csv.encodeRow(HEADER)+System.lineSeparator(),StandardCharsets.UTF_8);
        List<String> lines=Files.readAllLines(file,StandardCharsets.UTF_8);Map<String,Account> accounts=new LinkedHashMap<>();if(lines.isEmpty())return accounts;
        if(!Csv.parse(lines.get(0)).equals(HEADER))throw new IOException("Teacher account header is not recognized; data was not changed.");
        for(int i=1;i<lines.size();i++){if(lines.get(i).isBlank())continue;try{
            List<String> r=Csv.parse(lines.get(i));if(r.size()!=HEADER.size())throw new IllegalArgumentException("incorrect column count");
            String id=validId(r.get(0)),name=validName(r.get(1));Account a=new Account(new TeacherProfile(id,name),r.get(2),r.get(3),r.get(4),r.get(5));
            if(accounts.putIfAbsent(normalize(id),a)!=null)throw new IllegalArgumentException("duplicate Teacher ID");
        }catch(IllegalArgumentException ex){if(strict)throw new IOException("Teachers CSV line "+(i+1)+" is invalid; data was preserved: "+ex.getMessage(),ex);}}
        return accounts;
    }
    private void write(Map<String,Account> accounts)throws IOException{
        Files.createDirectories(file.getParent());List<String> rows=new ArrayList<>();rows.add(Csv.encodeRow(HEADER));
        for(Account a:accounts.values())rows.add(Csv.encodeRow(List.of(a.profile.getTeacherId(),a.profile.getDisplayName(),a.salt,a.hash,a.iterations,a.mustChange)));
        Path tmp=file.resolveSibling("teachers.csv.tmp");Files.write(tmp,rows,StandardCharsets.UTF_8);
        try{Files.move(tmp,file,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(IOException ex){Files.move(tmp,file,StandardCopyOption.REPLACE_EXISTING);}
    }
    private List<Invite> readInvites()throws IOException{List<String> header=List.of("code_hash","owner_teacher_id","used");Files.createDirectories(invitationsFile.getParent());if(!Files.exists(invitationsFile))Files.writeString(invitationsFile,Csv.encodeRow(header)+System.lineSeparator(),StandardCharsets.UTF_8);List<String> lines=Files.readAllLines(invitationsFile,StandardCharsets.UTF_8);List<Invite> out=new ArrayList<>();if(lines.isEmpty()||!Csv.parse(lines.get(0)).equals(header))throw new IOException("Teacher invitation data has an unknown header; no changes were made.");for(int i=1;i<lines.size();i++){if(lines.get(i).isBlank())continue;List<String> row;try{row=Csv.parse(lines.get(i));if(row.size()!=3||(!row.get(2).equals("true")&&!row.get(2).equals("false")))throw new IllegalArgumentException();out.add(new Invite(row.get(0),validId(row.get(1)),Boolean.parseBoolean(row.get(2))));}catch(RuntimeException ex){throw new IOException("Teacher invitation row "+(i+1)+" is malformed; data was preserved.",ex);}}return out;}
    private void writeInvites(List<Invite> invites)throws IOException{List<String> rows=new ArrayList<>();rows.add(Csv.encodeRow(List.of("code_hash","owner_teacher_id","used")));for(Invite i:invites)rows.add(Csv.encodeRow(List.of(i.hash,i.ownerId,Boolean.toString(i.used))));Path tmp=invitationsFile.resolveSibling("teacher-invitations.csv.tmp");Files.write(tmp,rows,StandardCharsets.UTF_8);try{Files.move(tmp,invitationsFile,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(IOException ex){Files.move(tmp,invitationsFile,StandardCopyOption.REPLACE_EXISTING);}}
    private static String hashCode(String value){try{return Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(java.security.NoSuchAlgorithmException ex){throw new IllegalStateException("SHA-256 is unavailable.",ex);}}
    private static boolean containsInvitationHash(List<Invite> invites,String hash){for(Invite invite:invites)if(invite.hash.equals(hash))return true;return false;}
    private static String validId(String v){String id=v==null?"":v.trim();if(!id.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,31}"))throw new IllegalArgumentException("Teacher ID must use 1–32 letters, digits, dots, underscores, or hyphens.");return id;}
    private static String validName(String v){String name=v==null?"":v.trim();if(name.isEmpty()||name.length()>80||name.chars().anyMatch(Character::isISOControl))throw new IllegalArgumentException("Display name must be 1–80 printable characters.");return name;}
    private static String normalize(String id){return id.trim().toLowerCase(Locale.ROOT);}
    private record Account(TeacherProfile profile,String salt,String hash,String iterations,String mustChange){}
    private record Invite(String hash,String ownerId,boolean used){}
}
