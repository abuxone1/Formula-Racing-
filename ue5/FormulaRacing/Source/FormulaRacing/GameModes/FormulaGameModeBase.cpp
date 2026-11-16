// Copyright Apex Dynamics Motorsport Studios. All Rights Reserved.

#include "FormulaGameModeBase.h"

AFormulaGameModeBase::AFormulaGameModeBase()
{
    PrimaryActorTick.bCanEverTick = true;
    CurrentRaceState = EFormulaRaceState::PreGrid;
    TotalLaps = 3;
    ActiveStartingLights = 0;
}

void AFormulaGameModeBase::BeginPlay()
{
    Super::BeginPlay();
    StartRaceCountdown();
}

void AFormulaGameModeBase::Tick(float DeltaSeconds)
{
    Super::Tick(DeltaSeconds);

    if (bCountdownActive)
    {
        CountdownTimer += DeltaSeconds;
        if (CountdownTimer < 1.0f)
        {
            ActiveStartingLights = 1;
        }
        else if (CountdownTimer < 2.0f)
        {
            ActiveStartingLights = 2;
        }
        else if (CountdownTimer < 3.0f)
        {
            ActiveStartingLights = 3;
        }
        else if (CountdownTimer < 4.0f)
        {
            ActiveStartingLights = 4;
        }
        else if (CountdownTimer < 5.0f)
        {
            ActiveStartingLights = 5;
        }
        else if (CountdownTimer >= 6.2f)
        {
            // Lights out! Green flag racing begins
            ActiveStartingLights = 6;
            bCountdownActive = false;
            CurrentRaceState = EFormulaRaceState::Racing;
        }
    }

    if (CurrentRaceState == EFormulaRaceState::Racing)
    {
        SessionElapsedSeconds += DeltaSeconds;
    }
}

void AFormulaGameModeBase::StartRaceCountdown()
{
    CurrentRaceState = EFormulaRaceState::CountdownLights;
    CountdownTimer = 0.0f;
    ActiveStartingLights = 0;
    bCountdownActive = true;
}

void AFormulaGameModeBase::TriggerChequeredFlag()
{
    CurrentRaceState = EFormulaRaceState::Finished;
}
