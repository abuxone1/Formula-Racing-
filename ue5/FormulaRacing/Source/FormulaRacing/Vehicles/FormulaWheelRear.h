// Copyright Apex Dynamics Motorsport Studios. All Rights Reserved.

#pragma once

#include "CoreMinimal.h"
#include "ChaosVehicleWheel.h"
#include "FormulaWheelRear.generated.h"

/**
 * UFormulaWheelRear
 * Open-wheel rear driven wheel with 405mm wide racing slick contact patch,
 * high longitudinal drive traction, and MGU-K brake coupling.
 */
UCLASS()
class FORMULARACING_API UFormulaWheelRear : public UChaosVehicleWheel
{
    GENERATED_BODY()

public:
    UFormulaWheelRear();
};
