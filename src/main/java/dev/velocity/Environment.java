package dev.velocity;

import java.awt.Color;

record Environment(String name,Color sky,Color horizon,Color ground,Color road,Color edge,Color stone) {
    private static Color c(int rgb){return new Color(rgb);}
    static final Environment[] ALL={
        new Environment("SUNSET COAST",c(0x2c9ecc),c(0xffdab3),c(0x168cba),c(0xc49b6c),c(0xf5ddb0),c(0xceb081)),
        new Environment("NEON CITY",c(0x0b102d),c(0x482367),c(0x141b36),c(0x29304c),c(0x3de5f1),c(0x202b4e)),
        new Environment("ALPINE SKYWAY",c(0x83bddb),c(0xecf5ff),c(0xb8dce9),c(0x8198b2),c(0xf2f8ff),c(0x68879c)),
        new Environment("VOLCANIC RIFT",c(0x261631),c(0xd06a40),c(0xd94624),c(0x473d4f),c(0xff9b47),c(0x553843))
    };
}
