package org.xodium.illyriabending.enums

/** Defines how an ability is activated by the player. */
internal enum class ActivationTypeEnum {
    /** One-shot cast with no duration (AirBlast, AirAgility). */
    INSTANT,

    /** Hold to charge, release to launch (AirBlade). */
    CHARGE,

    /** Activate to enable, activate again to disable (AirWheel). */
    TOGGLE,

    /** Active for a fixed duration then expires (AirScooter). */
    DURATION,
}
