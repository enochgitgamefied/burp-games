package dev.velocity;

enum Vehicle {
    RUNNING("RUNNING",0), BIKE("BIKE",12), CAR("CAR",24), SURFBOARD("SURFBOARD",8);
    final String label;final double bonus;
    Vehicle(String label,double bonus){this.label=label;this.bonus=bonus;}
}
