// Copyright Apex Dynamics Motorsport Studios. All Rights Reserved.

#pragma once

#include "CoreMinimal.h"
#include "ChaosVehicleWheel.h"
#include "FormulaWheelFront.generated.h"

/**
 * UFormulaWheelFront
 * Open-wheel front steered wheel with 18-inch rim dimensions (305mm width),
 * high lateral cornering stiffness, and stiff formula suspension travel.
 */
UCLASS()
class FORMULARACING_API UFormulaWheelFront : public UChaosVehicleWheel
{
    GENERATED_BODY()

public:
    UFormulaWheelFront();
};
