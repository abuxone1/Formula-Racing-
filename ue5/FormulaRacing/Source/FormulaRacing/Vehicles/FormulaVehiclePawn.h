// Copyright Apex Dynamics Motorsport Studios. All Rights Reserved.

#pragma once

#include "CoreMinimal.h"
#include "WheeledVehiclePawn.h"
#include "InputActionValue.h"
#include "FormulaVehiclePawn.generated.h"

class UCameraComponent;
class USpringArmComponent;
class UAudioComponent;
class UInputMappingContext;
class UInputAction;
class UChaosWheeledVehicleMovementComponent;

UENUM(BlueprintType)
enum class EFormulaCameraView : uint8
{
    ChaseCam    UMETA(DisplayName = "Chase Camera (Third Person)"),
    CockpitCam  UMETA(DisplayName = "Cockpit Camera (First Person)"),
    TCam        UMETA(DisplayName = "T-Cam (Onboard Airbox)")
};

/**
 * AFormulaVehiclePawn
 * High-performance open-wheel Formula race car pawn powered by Chaos Vehicles.
 * Features 8-speed seamless transmission, DRS, ERS hybrid boost, cockpit/chase cameras,
 * and high-fidelity aerodynamic telemetry.
 */
UCLASS()
class FORMULARACING_API AFormulaVehiclePawn : public AWheeledVehiclePawn
{
    GENERATED_BODY()

public:
    AFormulaVehiclePawn();

    virtual void BeginPlay() override;
    virtual void Tick(float DeltaSeconds) override;
    virtual void SetupPlayerInputComponent(class UInputComponent* PlayerInputComponent) override;

    // --- COMPONENT HIERARCHY ---

    UPROPERTY(VisibleAnywhere, BlueprintReadOnly, Category = "Formula|Camera")
    TObjectPtr<USpringArmComponent> ChaseSpringArm;

    UPROPERTY(VisibleAnywhere, BlueprintReadOnly, Category = "Formula|Camera")
    TObjectPtr<UCameraComponent> ChaseCamera;

    UPROPERTY(VisibleAnywhere, BlueprintReadOnly, Category = "Formula|Camera")
    TObjectPtr<UCameraComponent> CockpitCamera;

    UPROPERTY(VisibleAnywhere, BlueprintReadOnly, Category = "Formula|Camera")
    TObjectPtr<UCameraComponent> TCamera;

    UPROPERTY(VisibleAnywhere, BlueprintReadOnly, Category = "Formula|Audio")
    TObjectPtr<UAudioComponent> EngineSoundComponent;

    // --- TUNABLE VEHICLE SPECS ---

    // Total vehicle curb mass in kg (Formula regulation: 798 kg dry + fuel)
    UPROPERTY(EditAnywhere, BlueprintReadWrite, Category = "Formula|Physics Setup")
    float VehicleMassKg = 845.0f;

    // Peak ICE Torque scale multiplier
    UPROPERTY(EditAnywhere, BlueprintReadWrite, Category = "Formula|Physics Setup")
    float EngineTorqueMultiplier = 1.0f;

    // Peak Brake Torque in Nm (Formula carbon-carbon brakes: ~4,500 Nm per wheel)
    UPROPERTY(EditAnywhere, BlueprintReadWrite, Category = "Formula|Physics Setup")
    float MaxBrakeTorque = 4500.0f;

    // Front/Rear brake bias percentage (e.g. 56% front)
    UPROPERTY(EditAnywhere, BlueprintReadWrite, Category = "Formula|Physics Setup", meta = (ClampMin = "50.0", ClampMax = "65.0"))
    float BrakeBiasPercent = 56.0f;

    // --- TRANSMISSION & HYBRID BOOST ---

    UPROPERTY(EditAnywhere, BlueprintReadWrite, Category = "Formula|Transmission")
    bool bAutomaticTransmission = true;

    UPROPERTY(BlueprintReadOnly, Category = "Formula|Aero & DRS")
    bool bIsDRSOpen = false;

    UPROPERTY(BlueprintReadOnly, Category = "Formula|Hybrid ERS")
    bool bIsERSDeploying = false;

    UPROPERTY(BlueprintReadOnly, Category = "Formula|Hybrid ERS")
    float ERSEnergyRemaining = 1.0f; // 0.0 to 1.0

    // --- TELEMETRY READOUTS ---

    UPROPERTY(BlueprintReadOnly, Category = "Formula|Telemetry")
    float SpeedKph = 0.0f;

    UPROPERTY(BlueprintReadOnly, Category = "Formula|Telemetry")
    float EngineRpm = 4200.0f;

    UPROPERTY(BlueprintReadOnly, Category = "Formula|Telemetry")
    int32 CurrentGear = 1;

    UPROPERTY(BlueprintReadOnly, Category = "Formula|Telemetry")
    EFormulaCameraView CurrentCameraView = EFormulaCameraView::ChaseCam;

    // --- ENHANCED INPUT ACTIONS ---

    UPROPERTY(EditDefaultsOnly, BlueprintReadOnly, Category = "Formula|Input")
    TObjectPtr<UInputMappingContext> VehicleMappingContext;

    UPROPERTY(EditDefaultsOnly, BlueprintReadOnly, Category = "Formula|Input")
    TObjectPtr<UInputAction> ThrottleAction;

    UPROPERTY(EditDefaultsOnly, BlueprintReadOnly, Category = "Formula|Input")
    TObjectPtr<UInputAction> BrakeAction;

    UPROPERTY(EditDefaultsOnly, BlueprintReadOnly, Category = "Formula|Input")
    TObjectPtr<UInputAction> SteerAction;

    UPROPERTY(EditDefaultsOnly, BlueprintReadOnly, Category = "Formula|Input")
    TObjectPtr<UInputAction> HandbrakeAction;

    UPROPERTY(EditDefaultsOnly, BlueprintReadOnly, Category = "Formula|Input")
    TObjectPtr<UInputAction> ShiftUpAction;

    UPROPERTY(EditDefaultsOnly, BlueprintReadOnly, Category = "Formula|Input")
    TObjectPtr<UInputAction> ShiftDownAction;

    UPROPERTY(EditDefaultsOnly, BlueprintReadOnly, Category = "Formula|Input")
    TObjectPtr<UInputAction> ToggleCameraAction;

    UPROPERTY(EditDefaultsOnly, BlueprintReadOnly, Category = "Formula|Input")
    TObjectPtr<UInputAction> ToggleDRSAction;

    UPROPERTY(EditDefaultsOnly, BlueprintReadOnly, Category = "Formula|Input")
    TObjectPtr<UInputAction> ERSDeployAction;

    // --- BLUEPRINT CALLABLE FUNCTIONS ---

    UFUNCTION(BlueprintCallable, Category = "Formula|Controls")
    void SetThrottleInput(float Val);

    UFUNCTION(BlueprintCallable, Category = "Formula|Controls")
    void SetBrakeInput(float Val);

    UFUNCTION(BlueprintCallable, Category = "Formula|Controls")
    void SetSteeringInput(float Val);

    UFUNCTION(BlueprintCallable, Category = "Formula|Controls")
    void ShiftGearUp();

    UFUNCTION(BlueprintCallable, Category = "Formula|Controls")
    void ShiftGearDown();

    UFUNCTION(BlueprintCallable, Category = "Formula|Camera")
    void CycleCameraView();

    UFUNCTION(BlueprintCallable, Category = "Formula|Aero")
    void ToggleDRS();

    UFUNCTION(BlueprintCallable, Category = "Formula|Hybrid")
    void SetERSDeploy(bool bActive);

protected:
    void HandleThrottle(const FInputActionValue& Value);
    void HandleBrake(const FInputActionValue& Value);
    void HandleSteering(const FInputActionValue& Value);
    void HandleHandbrake(const FInputActionValue& Value);

    void UpdateAudioTelemetry();
    void ApplyAerodynamicDownforce(float DeltaSeconds);

    TObjectPtr<UChaosWheeledVehicleMovementComponent> ChaosMovementComponent;
};
