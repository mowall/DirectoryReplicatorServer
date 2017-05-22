package org.assignment.directoryreplicatorserver;

public class ApplicationStart {
   public static void main(String[] Args) {
      DirReplicatorServer dirReplicatorServer = new DirReplicatorServerImpl(6666);
      dirReplicatorServer.start();
   }
}
