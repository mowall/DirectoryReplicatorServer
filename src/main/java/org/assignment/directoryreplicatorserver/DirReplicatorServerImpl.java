package org.assignment.directoryreplicatorserver;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class DirReplicatorServerImpl implements DirReplicatorServer {
   private static final String userHomeDirectoryAttribute = "user.home";
   private static final String serverFolderName = "ServerFolder";
   private static final String dateFormat = "yyyyMMdd_HHmmss";
   private final int port;

   DirReplicatorServerImpl(int port) {
      this.port = port;
   }

   @Override
   public void start() {
      try {
         ServerSocket ss = new ServerSocket(port);
         while (true) {
            Socket s = ss.accept();
            ObjectInputStream objectInputStream = new ObjectInputStream(s.getInputStream());
            Boolean isInitial = objectInputStream.readBoolean();
            String directoryName = (String) objectInputStream.readObject();
            String userHomeDirectory = System.getProperty(userHomeDirectoryAttribute);
            String requiredPath = userHomeDirectory + File.separator + serverFolderName + File.separator + directoryName;
            if (isInitial) {
               createDirectory(requiredPath, directoryName);
            }
            Boolean isFilesToCreateEmpty = objectInputStream.readBoolean();
            if (!isFilesToCreateEmpty) {
               int folderCount = objectInputStream.readInt();
               if (folderCount > 0) {
                  for (int i = 0; i < folderCount; i++) {
                     String folderPath = (String) objectInputStream.readObject();
                     Path path = Paths.get(requiredPath + File.separator + folderPath);
                     if (!Files.exists(path)) {
                        Files.createDirectories(path);
                     }
                  }
               }
               int fileCount = objectInputStream.readInt();
               if (fileCount > 0) {
                  for (int i = 0; i < fileCount; i++) {
                     Long fileSize = objectInputStream.readLong();
                     String filePath = (String) objectInputStream.readObject();
                     Path path = Paths.get(requiredPath + File.separator + filePath);
                     if (Files.exists(path)) {
                        Files.delete(path);
                     }
                     if (fileSize > 0) {
                        byte[] mybytearray = new byte[fileSize.intValue()];
                        InputStream inputStream = s.getInputStream();
                        FileOutputStream fileOutputStream = new FileOutputStream(requiredPath + File.separator + filePath);
                        int bytesRead;
                        int current = 0;
                        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
                        bytesRead = inputStream.read(mybytearray, 0, mybytearray.length);
                        current = bytesRead;
                        do {
                           bytesRead = inputStream.read(mybytearray, current, (mybytearray.length - current));
                           if (bytesRead >= 0) current += bytesRead;
                        } while (bytesRead > -1 && current < fileSize);
                        bufferedOutputStream.write(mybytearray, 0, current);
                        bufferedOutputStream.flush();
                     } else if (fileSize == 0) {
                        FileOutputStream fileOutputStream = new FileOutputStream(requiredPath + File.separator + filePath);
                        fileOutputStream.write(("").getBytes());
                        fileOutputStream.close();
                     }
                  }
               }
            }

            //files to delete
            Boolean isFilesToDeleteEmpty = objectInputStream.readBoolean();
            if (!isFilesToDeleteEmpty) {
               int fileCount = objectInputStream.readInt();
               for (int i = 0; i < fileCount; i++) {
                  String filePath = (String) objectInputStream.readObject();
                  Path path = Paths.get(requiredPath + File.separator + filePath);
                  if (Files.exists(path)) {
                     deleteRecursive(new File(path.toString()));
                  }
               }
            }

            //files to modified
            Boolean isFilesToModifiedEmpty = objectInputStream.readBoolean();
            if (!isFilesToModifiedEmpty) {
               int fileCount = objectInputStream.readInt();
               for (int i = 0; i < fileCount; i++) {
                  Long fileSize = objectInputStream.readLong();
                  String filePath = (String) objectInputStream.readObject();
                  if (fileSize > 0) {
                     int bytesRead;
                     int current = 0;
                     byte[] myByteArray = new byte[fileSize.intValue()];
                     InputStream inputStream = s.getInputStream();
                     FileOutputStream fileOutputStream = new FileOutputStream(requiredPath + File.separator + filePath);
                     BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
                     bytesRead = inputStream.read(myByteArray, 0, myByteArray.length);
                     current = bytesRead;
                     do {
                        bytesRead = inputStream.read(myByteArray, current, (myByteArray.length - current));
                        if (bytesRead >= 0) current += bytesRead;
                     } while (bytesRead > -1 && current < fileSize);
                     bufferedOutputStream.write(myByteArray, 0, current);
                     bufferedOutputStream.flush();
                  } else if (fileSize == 0) {
                     FileOutputStream fileOutputStream = new FileOutputStream(requiredPath + File.separator + filePath);
                     fileOutputStream.write(("").getBytes());
                     fileOutputStream.close();
                  }
               }
            }
            s.close();
         }
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   private void createDirectory(String requiredPath, String directoryName) throws IOException {
      File directoryPath = new File(requiredPath);
      Path path = Paths.get(requiredPath);
      if (Files.exists(path)) {
         String timeStamp = new SimpleDateFormat(dateFormat).format(Calendar.getInstance().getTime());
         String newDirName = directoryName + "_another(renamedAt-" + timeStamp + ")";
         File newDir = new File(directoryPath.getParent() + File.separator + newDirName);
         directoryPath.renameTo(newDir);
      }
      Files.createDirectories(path);
   }

   private static boolean deleteRecursive(File path) throws FileNotFoundException {
      if (!path.exists()) throw new FileNotFoundException(path.getAbsolutePath());
      boolean returnFlag = true;
      if (path.isDirectory()) {
         for (File f : path.listFiles()) {
            returnFlag = returnFlag && deleteRecursive(f);
         }
      }
      return returnFlag && path.delete();
   }
}
