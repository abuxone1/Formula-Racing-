// Copyright Apex Dynamics Motorsport Studios. All Rights Reserved.

#include "FormulaVehiclePawn.h"
#include "ChaosWheeledVehicleMovementComponent.h"
#include "GameFramework/SpringArmComponent.h"
#include "Camera/CameraComponent.h"
#include "Components/AudioComponent.h"
#include "Components/SkeletalMeshComponent.h"
#include "EnhancedInputComponent.h"
#include "EnhancedInputSubsystems.h"
#include "Engine/LocalPlayer.h"

AFormulaVehiclePawn::AFormulaVehiclePawn()
{
    PrimaryActorTick.bCanEverTick = true;

    // 1. Get Chaos Movement Component
    ChaosMovementComponent = Cast<UChaosWheeledVehicleMovementComponent>(GetVehicleMovementComponent());

    // 2. Setup Third-Person Chase Spring Arm & Camera
    ChaseSpringArm = CreateDefaultSubobject<USpringArmComponent>(TEXT("ChaseSpringArm"));
    ChaseSpringArm->SetupAttachment(GetMesh());
    ChaseSpringArm->TargetArmLength = 650.0f;
    ChaseSpringArm->SocketOffset = FVector(0.0f, 0.0f, 180.0f);
    ChaseSpringArm->bUsePawnControlRotation = false;
    ChaseSpringArm->bInheritPitch = true;
    ChaseSpringArm->bInheritYaw = true;
    ChaseSpringArm->bInheritRoll = false;
    ChaseSpringArm->bEnableCameraLag = true;
    ChaseSpringArm->bEnableCameraRotationLag = true;
    ChaseSpringArm->CameraLagSpeed = 18.0f;
    ChaseSpringArm->CameraRotationLagSpeed = 14.0f;

    ChaseCamera = CreateDefaultSubobject<UCameraComponent>(TEXT("ChaseCamera"));
    ChaseCamera->SetupAttachment(ChaseSpringArm, USpringArmComponent::SocketName);
    ChaseCamera->FieldOfView = 75.0f;

    // 3. Setup First-Person Cockpit Camera (Inside Halo & Steering Wheel)
    CockpitCamera = CreateDefaultSubobject<UCameraComponent>(TEXT("CockpitCamera"));
    CockpitCamera->SetupAttachment(GetMesh());
    CockpitCamera->SetRelativeLocation(FVector(-25.0f, 0.0f, 75.0f));
    CockpitCamera->SetRelativeRotation(FRotator(0.0f, 0.0f, 0.0f));
    CockpitCamera->FieldOfView = 78.0f;
    CockpitCamera->bAutoActivate = false;

    // 4. Setup T-Cam (TV Onboard Camera mounted above airbox scoop)
    TCamera = CreateDefaultSubobject<UCameraComponent>(TEXT("TCamera"));
    TCamera->SetupAttachment(GetMesh());
    TCamera->SetRelativeLocation(FVector(-60.0f, 0.0f, 125.0f));
    TCamera->SetRelativeRotation(FRotator(-2.0f, 0.0f, 0.0f));
    TCamera->FieldOfView = 72.0f;
    TCamera->bAutoActivate = false;

    // 5. Setup Procedural Audio Component
    EngineSoundComponent = CreateDefaultSubobject<UAudioComponent>(TEXT("EngineSoundComponent"));
    EngineSoundComponent->SetupAttachment(GetMesh());
    EngineSoundComponent->bAutoActivate = true;

    CurrentCameraView = EFormulaCameraView::ChaseCam;
}

void AFormulaVehiclePawn::BeginPlay()
{
    Super::BeginPlay();

    // Register Enhanced Input Mapping Context
    if (APlayerController* PC = Cast<APlayerController>(GetController()))
    {
        if (UEnhancedInputLocalPlayerSubsystem* Subsystem = ULocalPlayer::GetSubsystem<UEnhancedInputLocalPlayerSubsystem>(PC->GetLocalPlayer()))
        {
            if (VehicleMappingContext)
            {
                Subsystem->AddMappingContext(VehicleMappingContext, 0);
            }
        }
    }

    if (ChaosMovementComponent)
    {
        ChaosMovementComponent->SetUseAutomaticGears(bAutomaticTransmission);
    }
}

void AFormulaVehiclePawn::Tick(float DeltaSeconds)
{
    Super::Tick(DeltaSeconds);

    if (ChaosMovementComponent)
    {
        SpeedKph = FMath::Abs(ChaosMovementComponent->GetForwardSpeedMPH()) * 1.60934f;
        EngineRpm = FMath::Clamp(ChaosMovementComponent->GetEngineRotationSpeed(), 4200.0f, 15000.0f);
        CurrentGear = ChaosMovementComponent->GetCurrentGear();
    }

    ApplyAerodynamicDownforce(DeltaSeconds);
    UpdateAudioTelemetry();

    // Regenerate or drain ERS battery
    if (bIsERSDeploying && ERSEnergyRemaining > 0.0f)
    {
        ERSEnergyRemaining = FMath::Max(0.0f, ERSEnergyRemaining - 0.08f * DeltaSeconds);
    }
    else if (!bIsERSDeploying && ERSEnergyRemaining < 1.0f)
    {
        // Harvest MGU-K energy under braking
        if (ChaosMovementComponent && ChaosMovementComponent->GetBrakeInput() > 0.1f)
        {
            ERSEnergyRemaining = FMath::Min(1.0f, ERSEnergyRemaining + 0.05f * DeltaSeconds);
        }
    }
}

void AFormulaVehiclePawn::SetupPlayerInputComponent(UInputComponent* PlayerInputComponent)
{
    Super::SetupPlayerInputComponent(PlayerInputComponent);

    if (UEnhancedInputComponent* EnhancedInputComponent = Cast<UEnhancedInputComponent>(PlayerInputComponent))
    {
        // Throttle & Brake
        if (ThrottleAction)
            EnhancedInputComponent->BindAction(ThrottleAction, ETriggerEvent::Triggered, this, &AFormulaVehiclePawn::HandleThrottle);
        if (BrakeAction)
            EnhancedInputComponent->BindAction(BrakeAction, ETriggerEvent::Triggered, this, &AFormulaVehiclePawn::HandleBrake);

        // Steering & Handbrake
        if (SteerAction)
            EnhancedInputComponent->BindAction(SteerAction, ETriggerEvent::Triggered, this, &AFormulaVehiclePawn::HandleSteering);
        if (HandbrakeAction)
            EnhancedInputComponent->BindAction(HandbrakeAction, ETriggerEvent::Triggered, this, &AFormulaVehiclePawn::HandleHandbrake);

        // Manual Transmission Gearshifts
        if (ShiftUpAction)
            EnhancedInputComponent->BindAction(ShiftUpAction, ETriggerEvent::Started, this, &AFormulaVehiclePawn::ShiftGearUp);
        if (ShiftDownAction)
            EnhancedInputComponent->BindAction(ShiftDownAction, ETriggerEvent::Started, this, &AFormulaVehiclePawn::ShiftGearDown);

        // Camera Toggle
        if (ToggleCameraAction)
            EnhancedInputComponent->BindAction(ToggleCameraAction, ETriggerEvent::Started, this, &AFormulaVehiclePawn::CycleCameraView);

        // DRS & ERS Actions
        if (ToggleDRSAction)
            EnhancedInputComponent->BindAction(ToggleDRSAction, ETriggerEvent::Started, this, &AFormulaVehiclePawn::ToggleDRS);
    }
}

void AFormulaVehiclePawn::HandleThrottle(const FInputActionValue& Value)
{
    const float AxisValue = Value.Get<float>();
    SetThrottleInput(AxisValue);
}

void AFormulaVehiclePawn::HandleBrake(const FInputActionValue& Value)
{
    const float AxisValue = Value.Get<float>();
    SetBrakeInput(AxisValue);
    // Tapping brakes closes DRS flap automatically
    if (AxisValue > 0.1f && bIsDRSOpen)
    {
        bIsDRSOpen = false;
    }
}

void AFormulaVehiclePawn::HandleSteering(const FInputActionValue& Value)
{
    const float AxisValue = Value.Get<float>();
    SetSteeringInput(AxisValue);
}

void AFormulaVehiclePawn::HandleHandbrake(const FInputActionValue& Value)
{
    const bool bHandbrake = Value.Get<bool>();
    if (ChaosMovementComponent)
    {
        ChaosMovementComponent->SetHandbrakeInput(bHandbrake);
    }
}

void AFormulaVehiclePawn::SetThrottleInput(float Val)
{
    if (ChaosMovementComponent)
    {
        float FinalThrottle = Val * EngineTorqueMultiplier;
        // Inject ERS boost (+160 hp electric motor)
        if (bIsERSDeploying && ERSEnergyRemaining > 0.02f)
        {
            FinalThrottle *= 1.25f;
        }
        ChaosMovementComponent->SetThrottleInput(FinalThrottle);
    }
}

void AFormulaVehiclePawn::SetBrakeInput(float Val)
{
    if (ChaosMovementComponent)
    {
        ChaosMovementComponent->SetBrakeInput(Val);
    }
}

void AFormulaVehiclePawn::SetSteeringInput(float Val)
{
    if (ChaosMovementComponent)
    {
        // High-speed steering damping: prevent snap oversteer at 300+ km/h
        const float SpeedDamp = FMath::Clamp(1.0f - (SpeedKph / 360.0f) * 0.55f, 0.45f, 1.0f);
        ChaosMovementComponent->SetSteeringInput(Val * SpeedDamp);
    }
}

void AFormulaVehiclePawn::ShiftGearUp()
{
    if (ChaosMovementComponent && !bAutomaticTransmission)
    {
        const int32 Target = ChaosMovementComponent->GetTargetGear();
        if (Target < 8)
        {
            ChaosMovementComponent->SetTargetGear(Target + 1, true);
        }
    }
}

void AFormulaVehiclePawn::ShiftGearDown()
{
    if (ChaosMovementComponent && !bAutomaticTransmission)
    {
        const int32 Target = ChaosMovementComponent->GetTargetGear();
        if (Target > -1)
        {
            ChaosMovementComponent->SetTargetGear(Target - 1, true);
        }
    }
}

void AFormulaVehiclePawn::CycleCameraView()
{
    ChaseCamera->SetActive(false);
    CockpitCamera->SetActive(false);
    TCamera->SetActive(false);

    switch (CurrentCameraView)
    {
    case EFormulaCameraView::ChaseCam:
        CurrentCameraView = EFormulaCameraView::CockpitCam;
        CockpitCamera->SetActive(true);
        break;
    case EFormulaCameraView::CockpitCam:
        CurrentCameraView = EFormulaCameraView::TCam;
        TCamera->SetActive(true);
        break;
    case EFormulaCameraView::TCam:
        CurrentCameraView = EFormulaCameraView::ChaseCam;
        ChaseCamera->SetActive(true);
        break;
    }
}

void AFormulaVehiclePawn::ToggleDRS()
{
    bIsDRSOpen = !bIsDRSOpen;
}

void AFormulaVehiclePawn::SetERSDeploy(bool bActive)
{
    bIsERSDeploying = bActive;
}

void AFormulaVehiclePawn::ApplyAerodynamicDownforce(float DeltaSeconds)
{
    if (!GetMesh() || !GetMesh()->IsSimulatingPhysics()) return;

    // Speed in m/s
    const float SpeedMps = SpeedKph / 3.6f;
    const float SpeedSq = SpeedMps * SpeedMps;

    // F_downforce = 0.5 * rho * Cl * A * v^2
    // High-downforce Formula generates ~1,400 kg (13,700 N) of downforce at 250 km/h
    const float BaseCl = 1.35f;
    const float DownforceNewtons = 0.5f * 1.225f * BaseCl * 2.2f * SpeedSq;

    // Apply downward aero force in vehicle local -Z direction
    const FVector DownforceVector = -GetActorUpVector() * (DownforceNewtons * 100.0f * DeltaSeconds); // cm/s^2 equivalent
    GetMesh()->AddForce(DownforceVector, NAME_None, true);

    // Aerodynamic Drag: F_drag = 0.5 * rho * Cd * A * v^2
    // DRS open reduces drag by 30%
    float Cd = 0.72f;
    if (bIsDRSOpen)
    {
        Cd *= 0.70f;
    }
    const float DragNewtons = 0.5f * 1.225f * Cd * 1.5f * SpeedSq;
    const FVector DragVector = -GetActorForwardVector() * (DragNewtons * 100.0f * DeltaSeconds);
    GetMesh()->AddForce(DragVector, NAME_None, true);
}

void AFormulaVehiclePawn::UpdateAudioTelemetry()
{
    if (EngineSoundComponent && EngineSoundComponent->IsPlaying())
    {
        // Map 4,000 - 15,000 RPM to sound pitch parameter
        const float PitchMultiplier = FMath::Clamp(EngineRpm / 9000.0f, 0.45f, 2.0f);
        EngineSoundComponent->SetFloatParameter(FName("RPM"), EngineRpm);
        EngineSoundComponent->SetFloatParameter(FName("Pitch"), PitchMultiplier);
        EngineSoundComponent->SetFloatParameter(FName("SpeedKph"), SpeedKph);
    }
}
