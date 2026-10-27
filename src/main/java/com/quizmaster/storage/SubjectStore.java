package com.quizmaster.storage;

import com.quizmaster.model.Subject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** CSV persistence for teacher-owned subjects. */
public final class SubjectStore {
    private static final List<String> HEADER=List.of("subject_id","subject_name","description","owner_teacher_id","active","created_at");
    private final Path file;
    public SubjectStore(Path directory){file=directory.resolve("subjects.csv");}
    public synchronized List<Subject> loadAll()throws IOException{return new ArrayList<>(read().values());}
    public synchronized Subject find(String id)throws IOException{return id==null?null:read().get(id.trim().toLowerCase(Locale.ROOT));}
    public synchronized Subject add(Subject subject)throws IOException{
        if(subject==null)throw new IllegalArgumentException("Subject is required.");Map<String,Subject> all=read();String key=key(subject.getId());
        if(all.containsKey(key))throw new IllegalArgumentException("That subject ID already exists.");
        for(Subject existing:all.values())if(existing.getName().equalsIgnoreCase(subject.getName())&&existing.getOwnerTeacherId().equalsIgnoreCase(subject.getOwnerTeacherId()))throw new IllegalArgumentException("You already own a subject with that name.");
        all.put(key,subject);write(all);return subject;
    }
    public synchronized Subject setActive(String id,boolean active)throws IOException{
        Map<String,Subject> all=read();String key=key(id);Subject old=all.get(key);if(old==null)throw new IllegalArgumentException("Subject was not found.");
        Subject updated=new Subject(old.getId(),old.getName(),old.getDescription(),old.getOwnerTeacherId(),active,old.getCreatedAt());all.put(key,updated);write(all);return updated;
    }
    /** Seeds only the one initial OOP subject, never overwriting an existing row. */
    public synchronized boolean ensureOopSubject(String ownerTeacherId)throws IOException{
        if(find(Subject.OOP_ID)!=null)return false;
        add(new Subject(Subject.OOP_ID,Subject.OOP_NAME,"SPPU PCC-202 OOP & Computer Graphics (2024 pattern)",ownerTeacherId,true,Instant.now()));return true;
    }
    private Map<String,Subject> read()throws IOException{
        Files.createDirectories(file.getParent());if(!Files.exists(file))Files.writeString(file,Csv.encodeRow(HEADER)+System.lineSeparator(),StandardCharsets.UTF_8);
        List<String> lines=Files.readAllLines(file,StandardCharsets.UTF_8);Map<String,Subject> all=new LinkedHashMap<>();if(lines.isEmpty())return all;
        if(!Csv.parse(lines.get(0)).equals(HEADER))throw new IOException("Subject CSV header is not recognized; data was not changed.");
        for(int i=1;i<lines.size();i++){if(lines.get(i).isBlank())continue;try{List<String> r=Csv.parse(lines.get(i));if(r.size()!=HEADER.size())throw new IllegalArgumentException("incorrect column count");
            Subject s=new Subject(r.get(0),r.get(1),r.get(2),r.get(3),Boolean.parseBoolean(r.get(4)),Instant.parse(r.get(5)));
            if(!r.get(4).equalsIgnoreCase("true")&&!r.get(4).equalsIgnoreCase("false"))throw new IllegalArgumentException("invalid active flag");
            if(all.putIfAbsent(key(s.getId()),s)!=null)throw new IllegalArgumentException("duplicate subject ID");
        }catch(RuntimeException ex){throw new IOException("Subject CSV line "+(i+1)+" is malformed; data was preserved: "+ex.getMessage(),ex);}}
        return all;
    }
    private void write(Map<String,Subject> all)throws IOException{
        List<String> rows=new ArrayList<>();rows.add(Csv.encodeRow(HEADER));for(Subject s:all.values())rows.add(Csv.encodeRow(List.of(s.getId(),s.getName(),s.getDescription(),s.getOwnerTeacherId(),Boolean.toString(s.isActive()),s.getCreatedAt().toString())));
        Path tmp=file.resolveSibling("subjects.csv.tmp");Files.write(tmp,rows,StandardCharsets.UTF_8);try{Files.move(tmp,file,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(IOException ex){Files.move(tmp,file,StandardCopyOption.REPLACE_EXISTING);}
    }
    private static String key(String id){if(id==null||id.isBlank())throw new IllegalArgumentException("Subject ID is required.");return id.trim().toLowerCase(Locale.ROOT);}
}
