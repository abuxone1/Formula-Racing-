// Copyright Apex Dynamics Motorsport Studios. All Rights Reserved.
// Project: Car Racing: FORMULA (Unreal Engine 5 / C++ Architecture)

#include "FormulaVehiclePhysics.h"
#include "Math/UnrealMathUtility.h"

UFormulaVehiclePhysicsComponent::UFormulaVehiclePhysicsComponent()
{
    PrimaryComponentTick.bCanEverTick = true;
    PrimaryComponentTick.TickGroup = TG_PrePhysics;

    // Initialize 4-wheel telemetry arrays
    WheelSuspensionDeflections.Init(0.0f, 4);
    WheelSlipAngles.Init(0.0f, 4);
}

void UFormulaVehiclePhysicsComponent::BeginPlay()
{
    Super::BeginPlay();
    CurrentEngineRpm = 4200.0f;
    CurrentGear = 1;
}

void UFormulaVehiclePhysicsComponent::TickComponent(float DeltaTime, ELevelTick TickType, FActorComponentTickFunction* ThisTickFunction)
{
    Super::TickComponent(DeltaTime, TickType, ThisTickFunction);

    UpdateSuspensionForces(DeltaTime);
    UpdateWheelFriction(DeltaTime);
    UpdatePowertrain(DeltaTime);
}

float UFormulaVehiclePhysicsComponent::CalculatePacejkaLateralForce(
    float SlipAngleDeg,
    float NormalLoadNewtons,
    const FPacejkaTireCoefficients& Coeffs,
    float CamberDeg) const
{
    // Convert slip angle to radians
    const float AlphaRad = FMath::DegreesToRadians(SlipAngleDeg);
    const float CamberRad = FMath::DegreesToRadians(CamberDeg);

    // Pacejka Magic Formula formulation:
    // y(x) = D * sin( C * atan( B * x - E * (B * x - atan(B * x)) ) )
    const float Bx = Coeffs.B_Stiffness * AlphaRad;
    const float AtanBx = FMath::Atan(Bx);
    const float InnerArg = Bx - Coeffs.E_Curvature * (Bx - AtanBx);
    const float FrictionCoeff = Coeffs.D_PeakFriction * FMath::Sin(Coeffs.C_Shape * FMath::Atan(InnerArg));

    // Base lateral cornering force
    float LateralForce = NormalLoadNewtons * FrictionCoeff;

    // Camber thrust addition
    const float CamberThrustForce = NormalLoadNewtons * Coeffs.CamberStiffness * CamberRad;
    LateralForce += CamberThrustForce;

    return LateralForce;
}

float UFormulaVehiclePhysicsComponent::CalculateSuspensionForce(
    float DeflectionMeters,
    float VelocityMps,
    const FSuspensionAxleDynamics& Dynamics) const
{
    // Hooke's Law spring force: F_spring = -k * x
    float SpringForce = Dynamics.SpringRate * DeflectionMeters;

    // Viscous damper rate: bump (compression, vel > 0) vs rebound (extension, vel < 0)
    const float DamperRate = (VelocityMps >= 0.0f) ? Dynamics.DamperBumpRate : Dynamics.DamperReboundRate;
    float DamperForce = DamperRate * VelocityMps;

    // Progressive elastomeric bump stop resistance when near bottoming out
    const float MaxTravelMeters = Dynamics.MaxTravelCm * 0.01f;
    if (DeflectionMeters > MaxTravelMeters * 0.85f)
    {
        const float BumpCompression = DeflectionMeters - (MaxTravelMeters * 0.85f);
        const float BumpStopForce = Dynamics.BumpStopStiffness * BumpCompression * BumpCompression * 100.0f;
        SpringForce += BumpStopForce;
    }

    return SpringForce + DamperForce;
}

float UFormulaVehiclePhysicsComponent::EvaluateEngineTorque(float ThrottleInput, float Rpm, bool bDeployErs) const
{
    const float ClampedThrottle = FMath::Clamp(ThrottleInput, 0.0f, 1.0f);
    const float ClampedRpm = FMath::Clamp(Rpm, 4000.0f, PowertrainSpecs.RevLimiterRpm);

    // 1. Internal Combustion Engine Torque evaluation
    float BaseTorque = 0.0f;
    if (PowertrainSpecs.EngineTorqueCurve)
    {
        BaseTorque = PowertrainSpecs.EngineTorqueCurve->GetFloatValue(ClampedRpm);
    }
    else
    {
        // Analytic torque curve: Peak torque at 11,500 RPM, broad racing plateau
        const float NormalizedRpm = (ClampedRpm - 4000.0f) / (PowertrainSpecs.RevLimiterRpm - 4000.0f);
        const float CurveShape = FMath::Sin(NormalizedRpm * PI);
        BaseTorque = PowertrainSpecs.MaxIceTorqueNm * (0.65f + 0.35f * CurveShape);
    }

    // 2. Turbocharger pressure boost multiplication
    const float BoostFactor = 1.0f + (CurrentTurboBoostBar / PowertrainSpecs.MaxTurboBoostBar) * 0.65f;
    float TotalIceTorque = BaseTorque * ClampedThrottle * BoostFactor;

    // 3. MGU-K Hybrid Electric deployment (+120 kW / 240 Nm instantaneous torque)
    if (bDeployErs && ClampedThrottle > 0.4f)
    {
        TotalIceTorque += PowertrainSpecs.ElectricBoostTorqueNm;
    }

    return TotalIceTorque;
}

void UFormulaVehiclePhysicsComponent::ShiftGear(bool bUp)
{
    if (bUp && CurrentGear < 8)
    {
        CurrentGear++;
    }
    else if (!bUp && CurrentGear > 1)
    {
        CurrentGear--;
    }
}

void UFormulaVehiclePhysicsComponent::UpdateSuspensionForces(float DeltaTime)
{
    // Compute suspension compression per wheel based on dynamic vertical load and roll/pitch
    for (int32 i = 0; i < 4; ++i)
    {
        const bool bIsFront = (i < 2);
        const FSuspensionAxleDynamics& Dynamics = bIsFront ? FrontSuspension : RearSuspension;
        const float DeflectionM = FMath::Clamp(WheelSuspensionDeflections[i] * 0.01f, -0.02f, Dynamics.MaxTravelCm * 0.01f);
        // Suspension force update
    }
}

void UFormulaVehiclePhysicsComponent::UpdateWheelFriction(float DeltaTime)
{
    // Update slip angles and grip for 4 wheels
}

void UFormulaVehiclePhysicsComponent::UpdatePowertrain(float DeltaTime)
{
    // Turbo lag spool simulation towards target boost
    const float TargetBoost = PowertrainSpecs.MaxTurboBoostBar;
    CurrentTurboBoostBar = FMath::FInterpTo(CurrentTurboBoostBar, TargetBoost, DeltaTime, 1.0f / PowertrainSpecs.TurboSpoolTimeSec);
}
