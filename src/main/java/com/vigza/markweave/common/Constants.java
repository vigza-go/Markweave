package com.vigza.markweave.common;

public class Constants {

  public static class UserType {
    public static final int DISABLED = 0;
    public static final int NORMAL = 1;
    public static final int ADMIN = 2;
    public static final int SUPADMIN = 3;

    public static boolean isValid(int value) {
      if (value < 0 || value > 3)
        return false;
      return true;
    }
  }

  public static class FsNodeType {
    public static final int FILE = 1;
    public static final int FOLDER = 2;
    public static final int SHORTCUT = 3;

    public static boolean isValid(int value) {
      if (value < 1 || value > 3)
        return false;
      return true;
    }
  }

  public static class CollaborationPermission {
    public static final int CREATOR = 1;
    public static final int READ_WRITE = 2;
    public static final int READ_ONLY = 3;

    public static boolean isValid(int value) {
      if (value < 1 || value > 3)
        return false;
      return true;
    }

  }

}
