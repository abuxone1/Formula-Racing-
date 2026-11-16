// Copyright Apex Dynamics Motorsport Studios. All Rights Reserved.

#pragma once

#include "CoreMinimal.h"
#include "GameFramework/GameModeBase.h"
#include "FormulaGameModeBase.generated.h"

UENUM(BlueprintType)
enum class EFormulaRaceState : uint8
{
    PreGrid         UMETA(DisplayName = "Pre-Grid Setup"),
    CountdownLights UMETA(DisplayName = "Starting Lights Countdown"),
    Racing          UMETA(DisplayName = "Green Flag Racing"),
    SafetyCar       UMETA(DisplayName = "Safety Car Deployed"),
    Finished        UMETA(DisplayName = "Chequered Flag Finished")
};

/**
 * AFormulaGameModeBase
 * Primary GameMode handling race flow, starting lights, session timer, and scoring.
 */
UCLASS()
class FORMULARACING_API AFormulaGameModeBase : public AGameModeBase
{
    GENERATED_BODY()

public:
    AFormulaGameModeBase();

    virtual void BeginPlay() override;
    virtual void Tick(float DeltaSeconds) override;

    UPROPERTY(EditAnywhere, BlueprintReadWrite, Category = "Formula|Session")
    int32 TotalLaps = 3;

    UPROPERTY(BlueprintReadOnly, Category = "Formula|Session")
    EFormulaRaceState CurrentRaceState = EFormulaRaceState::PreGrid;

    UPROPERTY(BlueprintReadOnly, Category = "Formula|Session")
    int32 ActiveStartingLights = 0; // 0=Off, 1..5=Red, 6=Lights Out

    UPROPERTY(BlueprintReadOnly, Category = "Formula|Session")
    float SessionElapsedSeconds = 0.0f;

    UFUNCTION(BlueprintCallable, Category = "Formula|Session")
    void StartRaceCountdown();

    UFUNCTION(BlueprintCallable, Category = "Formula|Session")
    void TriggerChequeredFlag();

protected:
    float CountdownTimer = 0.0f;
    bool bCountdownActive = false;
};
