package com.logcat.offline;

public class Main {

	public static void main(String[] args) {
		UIThread.getInstance().runUI(args);
        System.exit(0);
	}
}
