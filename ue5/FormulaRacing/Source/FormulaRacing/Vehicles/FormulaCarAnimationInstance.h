// Copyright Apex Dynamics Motorsport Studios. All Rights Reserved.

#pragma once

#include "CoreMinimal.h"
#include "VehicleAnimationInstance.h"
#include "FormulaCarAnimationInstance.generated.h"

class AFormulaVehiclePawn;

/**
 * UFormulaCarAnimationInstance
 * Drives Skeletal Mesh animation: wheel rotation, front steering angle,
 * suspension arm articulation, DRS flap actuation, and cockpit steering wheel.
 */
UCLASS()
class FORMULARACING_API UFormulaCarAnimationInstance : public UVehicleAnimationInstance
{
    GENERATED_BODY()

public:
    UFormulaCarAnimationInstance();

    virtual void NativeInitializeAnimation() override;
    virtual void NativeUpdateAnimation(float DeltaSeconds) override;

    UPROPERTY(BlueprintReadOnly, Category = "Formula|Anim")
    TObjectPtr<AFormulaVehiclePawn> VehiclePawn;

    UPROPERTY(BlueprintReadOnly, Category = "Formula|Anim")
    float SteeringWheelAngle = 0.0f; // Cockpit wheel rotation in degrees

    UPROPERTY(BlueprintReadOnly, Category = "Formula|Anim")
    float DRSFlapAngle = 0.0f; // 0 deg (closed) to 25 deg (open)
};
