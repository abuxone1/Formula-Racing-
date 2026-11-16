// Copyright Apex Dynamics Motorsport Studios. All Rights Reserved.

#include "FormulaWheelRear.h"

UFormulaWheelRear::UFormulaWheelRear()
{
    // Wheel Dimensions (Formula Regulation 18-inch rear slicks: 405/720-18)
    WheelRadius = 36.0f; // 36 cm radius (720 mm diameter)
    WheelWidth = 40.5f;  // 405 mm wide contact patch

    // Rear Driven Axle (Fixed heading, receives engine & ERS drive torque)
    MaxSteerAngle = 0.0f;
    bAffectedByEngine = true;
    bAffectedByBrakes = true;
    bAffectedByHandbrake = true;

    // Rear Suspension (Stiffer to withstand massive rear wing aerodynamic load)
    SuspensionMaxRaise = 5.0f;
    SuspensionMaxDrop = 4.0f;
    SuspensionDampingRatio = 0.90f;
    SpringRate = 210000.0f; // N/m

    // Wide Slick Rear Traction
    FrictionForceMultiplier = 1.95f;
    CorneringStiffness = 1100.0f;
    SideSlipModifier = 1.0f;
    SlipThreshold = 18.0f;
    SkidThreshold = 22.0f;

    // Rear Brake Caliper Torque (Balanced with front for 56/44 brake bias)
    MaxBrakeTorque = 3800.0f;
    MaxHandBrakeTorque = 4000.0f;
}
