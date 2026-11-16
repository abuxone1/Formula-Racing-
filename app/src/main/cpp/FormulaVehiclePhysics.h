// Copyright Apex Dynamics Motorsport Studios. All Rights Reserved.
// Project: Car Racing: FORMULA (Unreal Engine 5 / C++ Architecture)

#pragma once

#include "CoreMinimal.h"
#include "Components/ActorComponent.h"
#include "Curves/CurveFloat.h"
#include "FormulaVehiclePhysics.generated.h"

/**
 * Wheel identifiers for open-wheel Formula vehicle (FL, FR, RL, RR)
 */
UENUM(BlueprintType)
enum class EFormulaWheel : uint8
{
    FrontLeft   UMETA(DisplayName = "Front Left"),
    FrontRight  UMETA(DisplayName = "Front Right"),
    RearLeft    UMETA(DisplayName = "Rear Left"),
    RearRight   UMETA(DisplayName = "Rear Right")
};

/**
 * Pacejka Magic Formula coefficients for tire lateral and longitudinal friction
 */
USTRUCT(BlueprintType)
struct FPacejkaTireCoefficients
{
    GENERATED_BODY()

    UPROPERTY(EditAnywhere, BlueprintReadWrite, Category = "Formula|Pacejka")
    float B_Stiffness = 10.0f;

    UPROPERTY(EditAnywhere, BlueprintReadWrite, Category = "Formula|Pacejka")
    float C_Shape = 1.65f;

    UPROPERTY(EditAnywhere, BlueprintReadWrite, Category = "Formula|Pacejka")
    float D_PeakFriction = 1.75f;

    UPROPERTY(EditAnywhere, BlueprintReadWrite, Category = "Formula|Pacejka")
    float E_Curvature = -0.80f;

    // Camber thrust sensitivity coefficient
    UPROPERTY(EditAnywhere, BlueprintReadWrite, Category = "Formula|Pacejka")
    float CamberStiffness = 0.085f;
};

/**
 * Suspension spring and damper dynamic parameters per axle
 */
USTRUCT(BlueprintType)
struct FSuspensionAxleDynamics
{
    GENERATED_BODY()

    // Spring stiffness in N/m (Formula high downforce setup: 140,000 - 220,000 N/m)
    UPROPERTY(EditAnywhere, BlueprintReadWrite, Category = "Formula|Suspension")
    float SpringRate = 180000.0f;

    // Fast and slow bump damping in Ns/m
    UPROPERTY(EditAnywhere, BlueprintReadWrite, Category = "Formula|Suspension")
    float DamperBumpRate = 7500.0f;

    // Fast and slow rebound damping in Ns/m
    UPROPERTY(EditAnywhere, BlueprintReadWrite, Category = "Formula|Suspension")
    float DamperReboundRate = 9500.0f;

    // Anti-Roll Bar (ARB) stiffness in Nm/deg
    UPROPERTY(EditAnywhere, BlueprintReadWrite, Category = "Formula|Suspension")
    float AntiRollBarRate = 45000.0f;

    // Maximum suspension travel distance in centimeters
    UPROPERTY(EditAnywhere, BlueprintReadWrite, Category = "Formula|Suspension")
    float MaxTravelCm = 5.5f;

    // Bump stop stiffness multiplier when suspension bottoms out
    UPROPERTY(EditAnywhere, BlueprintReadWrite, Category = "Formula|Suspension")
    float BumpStopStiffness = 450000.0f;
};

/**
 * Formula 1.6L V6 Turbo Hybrid Powertrain parameters
 */
USTRUCT(BlueprintType)
struct FPowertrainTorqueSpecs
{
    GENERATED_BODY()

    // Base engine torque curve asset (Torque Nm vs RPM 4,000 to 15,000)
    UPROPERTY(EditAnywhere, BlueprintReadWrite, Category = "Formula|Engine")
    TObjectPtr<UCurveFloat> EngineTorqueCurve;

    // Maximum ICE Peak Torque in Nm (around 420 Nm @ 11,500 RPM)
    UPROPERTY(EditAnywhere, BlueprintReadWrite, Category = "Formula|Engine")
    float MaxIceTorqueNm = 425.0f;

    // MGU-K Electric hybrid boost torque in Nm (instantaneous +240 Nm / 120 kW)
    UPROPERTY(EditAnywhere, BlueprintReadWrite, Category = "Formula|Hybrid")
    float ElectricBoostTorqueNm = 240.0f;

    // Turbocharger maximum gauge boost in Bar (approx 3.5 - 4.5 Bar)
    UPROPERTY(EditAnywhere, BlueprintReadWrite, Category = "Formula|Engine")
    float MaxTurboBoostBar = 4.2f;

    // Turbo spool inertia time constant in seconds
    UPROPERTY(EditAnywhere, BlueprintReadWrite, Category = "Formula|Engine")
    float TurboSpoolTimeSec = 0.18f;

    // Rev limiter ceiling in RPM
    UPROPERTY(EditAnywhere, BlueprintReadWrite, Category = "Formula|Engine")
    float RevLimiterRpm = 15000.0f;

    // 8-speed seamless shift transmission ratios
    UPROPERTY(EditAnywhere, BlueprintReadWrite, Category = "Formula|Transmission")
    TArray<float> GearRatios = { 0.0f, 3.25f, 2.50f, 2.02f, 1.66f, 1.41f, 1.23f, 1.09f, 0.97f };

    // Final drive axle ratio
    UPROPERTY(EditAnywhere, BlueprintReadWrite, Category = "Formula|Transmission")
    float FinalDriveRatio = 3.85f;
};

/**
 * UFormulaVehiclePhysicsComponent
 * Core C++ simulation component handling wheel friction, suspension dynamics,
 * and engine torque for open-wheel Formula race cars in Unreal Engine 5.
 */
UCLASS(ClassGroup=(Custom), meta=(BlueprintSpawnableComponent), Blueprintable, BlueprintType)
class FORMULARACING_API UFormulaVehiclePhysicsComponent : public UActorComponent
{
    GENERATED_BODY()

public:
    UFormulaVehiclePhysicsComponent();

    virtual void BeginPlay() override;
    virtual void TickComponent(float DeltaTime, ELevelTick TickType, FActorComponentTickFunction* ThisTickFunction) override;

    // --- TIRE FRICTION CONFIGURATION ---
    UPROPERTY(EditAnywhere, BlueprintReadWrite, Category = "Formula|Wheel Friction")
    FPacejkaTireCoefficients FrontTirePacejka;

    UPROPERTY(EditAnywhere, BlueprintReadWrite, Category = "Formula|Wheel Friction")
    FPacejkaTireCoefficients RearTirePacejka;

    // --- SUSPENSION DYNAMICS CONFIGURATION ---
    UPROPERTY(EditAnywhere, BlueprintReadWrite, Category = "Formula|Suspension Dynamics")
    FSuspensionAxleDynamics FrontSuspension;

    UPROPERTY(EditAnywhere, BlueprintReadWrite, Category = "Formula|Suspension Dynamics")
    FSuspensionAxleDynamics RearSuspension;

    // --- POWERTRAIN CONFIGURATION ---
    UPROPERTY(EditAnywhere, BlueprintReadWrite, Category = "Formula|Powertrain & Torque")
    FPowertrainTorqueSpecs PowertrainSpecs;

    // --- SIMULATION RUNTIME OUTPUTS ---
    UPROPERTY(BlueprintReadOnly, Category = "Formula|Telemetry")
    float CurrentEngineRpm = 4200.0f;

    UPROPERTY(BlueprintReadOnly, Category = "Formula|Telemetry")
    int32 CurrentGear = 1;

    UPROPERTY(BlueprintReadOnly, Category = "Formula|Telemetry")
    float CurrentTurboBoostBar = 0.0f;

    UPROPERTY(BlueprintReadOnly, Category = "Formula|Telemetry")
    float LateralGForce = 0.0f;

    UPROPERTY(BlueprintReadOnly, Category = "Formula|Telemetry")
    float LongitudinalGForce = 0.0f;

    UPROPERTY(BlueprintReadOnly, Category = "Formula|Telemetry")
    TArray<float> WheelSuspensionDeflections; // 4 wheels in cm

    UPROPERTY(BlueprintReadOnly, Category = "Formula|Telemetry")
    TArray<float> WheelSlipAngles;           // 4 wheels in degrees

    // --- BLUEPRINT CALLABLE PHYSICS METHODS ---

    /**
     * Calculates lateral cornering force using Pacejka Magic Formula:
     * Fy = D * sin(C * atan(B * alpha - E * (B * alpha - atan(B * alpha))))
     */
    UFUNCTION(BlueprintPure, Category = "Formula|Physics")
    float CalculatePacejkaLateralForce(float SlipAngleDeg, float NormalLoadNewtons, const FPacejkaTireCoefficients& Coeffs, float CamberDeg) const;

    /**
     * Calculates suspension spring and damping force for a wheel given deflection and velocity
     */
    UFUNCTION(BlueprintPure, Category = "Formula|Physics")
    float CalculateSuspensionForce(float DeflectionMeters, float VelocityMps, const FSuspensionAxleDynamics& Dynamics) const;

    /**
     * Evaluates total drive torque delivered to the driven rear wheels at current RPM and throttle
     */
    UFUNCTION(BlueprintPure, Category = "Formula|Physics")
    float EvaluateEngineTorque(float ThrottleInput, float Rpm, bool bDeployErs) const;

    /**
     * Shifts gear up or down
     */
    UFUNCTION(BlueprintCallable, Category = "Formula|Transmission")
    void ShiftGear(bool bUp);

protected:
    void UpdateSuspensionForces(float DeltaTime);
    void UpdateWheelFriction(float DeltaTime);
    void UpdatePowertrain(float DeltaTime);

    float TargetTurboBoost = 0.0f;
};
