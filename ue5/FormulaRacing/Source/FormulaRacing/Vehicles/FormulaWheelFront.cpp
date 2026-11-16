// Copyright Apex Dynamics Motorsport Studios. All Rights Reserved.

#include "FormulaWheelFront.h"

UFormulaWheelFront::UFormulaWheelFront()
{
    // Wheel Dimensions (Formula Regulation 18-inch front slicks: 305/720-18)
    WheelRadius = 36.0f; // 36 cm radius (720 mm diameter)
    WheelWidth = 30.5f;  // 305 mm tread width

    // Front Steered Axle (28 degrees maximum lock)
    MaxSteerAngle = 28.0f;
    bAffectedByEngine = false;
    bAffectedByBrakes = true;
    bAffectedByHandbrake = false;

    // Open-Wheel Suspension (Very stiff, short travel: 5 cm compression, 4 cm droop)
    SuspensionMaxRaise = 5.0f;
    SuspensionMaxDrop = 4.0f;
    SuspensionDampingRatio = 0.85f;
    SpringRate = 180000.0f; // N/m

    // High Downforce Formula Slick Friction
    FrictionForceMultiplier = 1.85f;
    CorneringStiffness = 1000.0f;
    SideSlipModifier = 1.0f;
    SlipThreshold = 20.0f;
    SkidThreshold = 24.0f;

    // Carbon-carbon brake torque per front caliper (Nm)
    MaxBrakeTorque = 4800.0f;
}
