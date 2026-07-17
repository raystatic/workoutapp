package com.workoutapp.composeapp.di

import com.workoutapp.composeapp.data.analytics.RestTimerExperimentRepository
import com.workoutapp.composeapp.data.analytics.RestTimerExperimentRepositoryImpl
import com.workoutapp.composeapp.data.db.DatabaseDriverFactory
import com.workoutapp.composeapp.data.db.SetType
import com.workoutapp.composeapp.data.db.WorkoutPrivacy
import com.workoutapp.composeapp.data.db.enumColumnAdapter
import com.workoutapp.composeapp.data.db.stringListAdapter
import com.workoutapp.composeapp.data.library.ExerciseRepository
import com.workoutapp.composeapp.data.library.ExerciseRepositoryImpl
import com.workoutapp.composeapp.data.library.ExerciseSeeder
import com.workoutapp.composeapp.data.onboarding.OnboardingRepository
import com.workoutapp.composeapp.data.onboarding.OnboardingRepositoryImpl
import com.workoutapp.composeapp.data.profile.UserProfileRepository
import com.workoutapp.composeapp.data.profile.UserProfileRepositoryImpl
import com.workoutapp.composeapp.data.progress.BodyMeasurementRepository
import com.workoutapp.composeapp.data.progress.BodyMeasurementRepositoryImpl
import com.workoutapp.composeapp.data.progress.PersonalRecordRepository
import com.workoutapp.composeapp.data.progress.PersonalRecordRepositoryImpl
import com.workoutapp.composeapp.data.resttimer.RestTimerSettingsRepository
import com.workoutapp.composeapp.data.resttimer.RestTimerSettingsRepositoryImpl
import com.workoutapp.composeapp.data.routines.RoutineExerciseRepository
import com.workoutapp.composeapp.data.routines.RoutineExerciseRepositoryImpl
import com.workoutapp.composeapp.data.routines.RoutineRepository
import com.workoutapp.composeapp.data.routines.RoutineRepositoryImpl
import com.workoutapp.composeapp.data.routines.RoutineSetRepository
import com.workoutapp.composeapp.data.routines.RoutineSetRepositoryImpl
import com.workoutapp.composeapp.data.workout.PreviousSetResolver
import com.workoutapp.composeapp.data.workout.WorkoutExerciseRepository
import com.workoutapp.composeapp.data.workout.WorkoutExerciseRepositoryImpl
import com.workoutapp.composeapp.data.workout.WorkoutRepository
import com.workoutapp.composeapp.data.workout.WorkoutRepositoryImpl
import com.workoutapp.composeapp.data.workout.WorkoutSetRepository
import com.workoutapp.composeapp.data.workout.WorkoutSetRepositoryImpl
import com.workoutapp.composeapp.db.AppDatabase
import com.workoutapp.composeapp.db.Exercise
import com.workoutapp.composeapp.db.RoutineSet
import com.workoutapp.composeapp.db.Workout
import com.workoutapp.composeapp.db.WorkoutSet
import com.workoutapp.composeapp.ui.activeworkout.ActiveWorkoutStore
import com.workoutapp.composeapp.ui.customexercise.AddCustomExerciseStore
import com.workoutapp.composeapp.ui.exercisedetail.ExerciseDetailStore
import com.workoutapp.composeapp.ui.finishworkout.FinishWorkoutStore
import com.workoutapp.composeapp.ui.resttimer.RestTimerController
import com.workoutapp.composeapp.ui.resttimer.RestTimerStore
import com.workoutapp.composeapp.ui.routinebuilder.RoutineBuilderStore
import com.workoutapp.composeapp.ui.walkthrough.WalkthroughStore
import com.workoutapp.composeapp.ui.workout.WorkoutStore
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module

/** Platform-specific bindings (currently just [DatabaseDriverFactory]). */
expect fun platformModule(): Module

val appModule = module {
    single {
        AppDatabase(
            driver = get<DatabaseDriverFactory>().createDriver(),
            exerciseAdapter = Exercise.Adapter(secondaryMusclesAdapter = stringListAdapter),
            routineSetAdapter = RoutineSet.Adapter(setTypeAdapter = enumColumnAdapter<SetType>()),
            workoutAdapter = Workout.Adapter(
                privacyAdapter = enumColumnAdapter<WorkoutPrivacy>(),
                mediaAdapter = stringListAdapter,
            ),
            workoutSetAdapter = WorkoutSet.Adapter(setTypeAdapter = enumColumnAdapter<SetType>()),
        )
    }
    single<ExerciseRepository> { ExerciseRepositoryImpl(get()) }
    single { ExerciseSeeder(get()) }
    single<RoutineRepository> { RoutineRepositoryImpl(get()) }
    single<RoutineExerciseRepository> { RoutineExerciseRepositoryImpl(get()) }
    single<RoutineSetRepository> { RoutineSetRepositoryImpl(get()) }
    single<WorkoutRepository> { WorkoutRepositoryImpl(get()) }
    single<WorkoutExerciseRepository> { WorkoutExerciseRepositoryImpl(get()) }
    single<WorkoutSetRepository> { WorkoutSetRepositoryImpl(get()) }
    single<BodyMeasurementRepository> { BodyMeasurementRepositoryImpl(get()) }
    single<PersonalRecordRepository> { PersonalRecordRepositoryImpl(get()) }
    single<UserProfileRepository> { UserProfileRepositoryImpl(get()) }
    single<RestTimerSettingsRepository> { RestTimerSettingsRepositoryImpl(get()) }
    single<OnboardingRepository> { OnboardingRepositoryImpl(get()) }
    single<RestTimerExperimentRepository> { RestTimerExperimentRepositoryImpl(get()) }
    single { PreviousSetResolver(get(), get()) }
    single { WorkoutStore(get(), get(), get(), get(), get(), get(), get()) }
    single { WalkthroughStore(get()) }
    single { RestTimerStore(get()) } bind RestTimerController::class
    factory { (workoutId: Long) -> ActiveWorkoutStore(workoutId, get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    factory { (workoutId: Long) -> FinishWorkoutStore(workoutId, get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    factory { (routineId: Long) -> RoutineBuilderStore(routineId, get(), get(), get(), get()) }
    factory { (exerciseId: Long) -> ExerciseDetailStore(exerciseId, get(), get()) }
    factory { AddCustomExerciseStore(get()) }
}
