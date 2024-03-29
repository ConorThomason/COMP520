package miniJava.CodeGenerator;
/*
 * @(#)RuntimeEntity.java                        2.0 1999/08/11
 *
 * Copyright (C) 1999 D.A. Watt and D.F. Brown
 * Dept. of Computing Science, University of Glasgow, Glasgow G12 8QQ Scotland
 * and School of Computer and Math Sciences, The Robert Gordon University,
 * St. Andrew Street, Aberdeen AB25 1HG, Scotland.
 * All rights reserved.
 *
 * This software is provided free for educational use only. It may
 * not be used for commercial purposes without the prior written permission
 * of the authors.
 */


// Run-time object

public class RuntimeEntity {

    public final static int maxRoutineLevel = 7;
    public int memoryOffset;

    public RuntimeEntity (int offset) {
        this.memoryOffset = offset;
    }

    public int size;

}