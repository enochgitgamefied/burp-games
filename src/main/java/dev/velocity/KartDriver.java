package dev.velocity;

import java.awt.Color;

/** Original procedural fan racers, sharing the Sonic rig and car geometry. */
enum KartDriver {
    SONIC("Sonic",0x1854db,0x2e6fe5,96,1.00,"Fast on the straights"),
    TAILS("Tails",0xf4a12f,0xf18d25,92,1.13,"Quick, light steering"),
    KNUCKLES("Knuckles",0xc92e43,0xd63143,94,.97,"Strong through contact"),
    AMY("Amy",0xf17daf,0xd84b8a,93,1.08,"Drift to charge boost");
    final String label,description;final Color body,livery;final double topSpeed,handling;
    KartDriver(String label,int body,int livery,double speed,double handling,String description){this.label=label;this.body=new Color(body);this.livery=new Color(livery);topSpeed=speed;this.handling=handling;this.description=description;}
}
