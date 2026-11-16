// Copyright Apex Dynamics Motorsport Studios. All Rights Reserved.

#include "FormulaCarAnimationInstance.h"
#include "FormulaVehiclePawn.h"

UFormulaCarAnimationInstance::UFormulaCarAnimationInstance()
{
}

void UFormulaCarAnimationInstance::NativeInitializeAnimation()
{
    Super::NativeInitializeAnimation();
    VehiclePawn = Cast<AFormulaVehiclePawn>(GetOwningActor());
}

void UFormulaCarAnimationInstance::NativeUpdateAnimation(float DeltaSeconds)
{
    Super::NativeUpdateAnimation(DeltaSeconds);

    if (!VehiclePawn)
    {
        VehiclePawn = Cast<AFormulaVehiclePawn>(GetOwningActor());
    }

    if (VehiclePawn)
    {
        // Animate cockpit steering wheel rotation (scales to ~90 degrees lock)
        // Steer angle is retrieved from pawn movement
        const float TargetSteeringAngle = VehiclePawn->GetVehicleMovementComponent() ?
            VehiclePawn->GetVehicleMovementComponent()->GetSteeringInput() * 95.0f : 0.0f;

        SteeringWheelAngle = FMath::FInterpTo(SteeringWheelAngle, TargetSteeringAngle, DeltaSeconds, 25.0f);

        // Animate DRS rear flap lift (0 to 25 degrees)
        const float TargetFlapAngle = VehiclePawn->bIsDRSOpen ? 25.0f : 0.0f;
        DRSFlapAngle = FMath::FInterpTo(DRSFlapAngle, TargetFlapAngle, DeltaSeconds, 15.0f);
    }
}
