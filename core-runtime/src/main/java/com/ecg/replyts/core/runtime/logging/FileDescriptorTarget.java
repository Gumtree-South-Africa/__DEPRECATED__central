package com.ecg.replyts.core.runtime.logging;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;

public enum FileDescriptorTarget {

    FileDescriptorOut("FileDescriptor.out", new PrintStream(new FileOutputStream(FileDescriptor.out), true)),

    FileDescriptorErr("FileDescriptor.err", new PrintStream(new FileOutputStream(FileDescriptor.err), true));

    public static FileDescriptorTarget findByName(String name) {
        for (FileDescriptorTarget target : FileDescriptorTarget.values()) {
            if (target.name.equalsIgnoreCase(name)) {
                return target;
            }
        }
        throw new IllegalArgumentException("[" + name + "] should be one of " + Arrays.toString(FileDescriptorTarget.values()));
    }

    private final String name;
    private final OutputStream stream;

    private FileDescriptorTarget(String name, OutputStream stream) {
        this.name = name;
        this.stream = stream;
    }

    public String getName() {
        return name;
    }

    public OutputStream getStream() {
        return stream;
    }

    @Override
    public String toString() {
        return name;
    }
}
