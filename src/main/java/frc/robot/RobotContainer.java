package frc.robot;

import java.io.IOException;
import java.time.Instant;
import java.util.function.DoubleSupplier;

import com.pathplanner.lib.commands.PathPlannerAuto;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.GenericHID;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.XboxController;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.CommandScheduler;
import edu.wpi.first.wpilibj2.command.InstantCommand;
import edu.wpi.first.wpilibj2.command.PrintCommand;
import edu.wpi.first.wpilibj2.command.button.JoystickButton;
import edu.wpi.first.wpilibj2.command.button.POVButton;

import frc.robot.autos.*;
import frc.robot.commands.intake.IntakeAndHold;
import frc.robot.commands.intake.IntakeControl;
import frc.robot.commands.swerve.Reset;
import frc.robot.commands.swerve.TeleopSwerve;
import frc.robot.commands.swerve.pid.*;
import frc.robot.commands.vision.AlignWithAprilTag;
// import frc.robot.commands.vision.DriveToAprilTag;
import frc.robot.commands.vision.GetAprilTagPose;
import frc.robot.commands.vision.SupplyAprilTagPose;
import frc.robot.commands.shooter.PrimeAndShoot;
import frc.robot.commands.shooter.PrimeWhileThenShoot;
import frc.robot.commands.shooter.TeleopShoot;
import frc.robot.subsystems.*;

/**
 * This class is where the bulk of the robot should be declared. Since Command-based is a
 * "declarative" paradigm, very little robot logic should actually be handled in the {@link Robot}
 * periodic methods (other than the scheduler calls). Instead, the structure of the robot (including
 * subsystems, commands, and button mappings) should be declared here.
 */
public class RobotContainer {
    /* Controllers */
    private final Joystick driver = new Joystick(0);
    private final Joystick driver2 = new Joystick(1);

    /* Joystick Axes */
    private final int leftThumbXID = XboxController.Axis.kLeftX.value;
    private final int leftThumbYID = XboxController.Axis.kLeftY.value;
    private final int rightThumbXID = XboxController.Axis.kRightX.value;

    private final int leftTriggerID = XboxController.Axis.kLeftTrigger.value;
    private final int rightTriggerID = XboxController.Axis.kRightTrigger.value;

    /* Driver Buttons */
    private final JoystickButton kX = new JoystickButton(driver, XboxController.Button.kX.value);
    private final JoystickButton kY = new JoystickButton(driver, XboxController.Button.kY.value);
    private final JoystickButton kA = new JoystickButton(driver, XboxController.Button.kA.value);
    private final JoystickButton kB = new JoystickButton(driver, XboxController.Button.kB.value);

    private final JoystickButton rightBumper = new JoystickButton(driver, XboxController.Button.kRightBumper.value);
    private final JoystickButton leftBumper = new JoystickButton(driver, XboxController.Button.kLeftBumper.value);
    
    private final POVButton DPadUp = new POVButton(driver, 0);
    private final POVButton DPadDown = new POVButton(driver, 180);
    private final POVButton DPadLeft = new POVButton(driver, 270);
    private final POVButton DPadRight = new POVButton(driver, 90);

    private final POVButton DPadUp2 = new POVButton(driver2, 0);
    private final POVButton DPadDown2 = new POVButton(driver2, 180);

    private final JoystickButton kY2 = new JoystickButton(driver2, XboxController.Button.kY.value);
    private final JoystickButton kA2 = new JoystickButton(driver2, XboxController.Button.kA.value);
    private final JoystickButton kB2 = new JoystickButton(driver2, XboxController.Button.kB.value);

    private boolean testCommandIsRunning = false;

    /* Subsystems */
    private final Swerve s_Swerve = new Swerve();
    private final Shooter s_Shooter = new Shooter();
    private final Intake s_Intake = new Intake();
    private final Climber s_Climber = new Climber();
    private final Vision s_Vision;

    private Command testCommand = new Spin(s_Swerve, () -> new Pose2d(0, 0, new Rotation2d(90)));

    /** The container for the robot. Contains subsystems, OI devices, and commands. */
    public RobotContainer() throws IOException {
        CommandScheduler.getInstance().onCommandInitialize(command -> System.out.println("Command interrupted: " + command.getName()));
        CommandScheduler.getInstance().onCommandInterrupt(command -> System.out.println("Command interrupted: " + command.getName()));
        CommandScheduler.getInstance().onCommandFinish(command -> System.out.println("Command finished: " + command.getName()));

        s_Vision = new Vision();
        s_Vision.resetPose();
        
        s_Swerve.setDefaultCommand(
            new TeleopSwerve(
                s_Swerve, 
                () -> driver.getRawAxis(leftThumbYID), // translation axis
                () -> driver.getRawAxis(leftThumbXID), // strafe axis
                () -> -driver.getRawAxis(rightThumbXID),  // rotation axis
                () -> kY.getAsBoolean()
            )
        );

        s_Shooter.setDefaultCommand(
            new TeleopShoot(
                s_Shooter, 
                () -> driver.getRawAxis(leftTriggerID),
                () -> driver.getRawAxis(rightTriggerID)
            )
        );

        // s_Vision.setDefaultCommand(
            // new SupplyAprilTagPose(s_Vision, new Pose2d(), (pose) -> targetPose = pose)
        // );

        s_Swerve.resetModulesToAbsolute();

        DriverStation.silenceJoystickConnectionWarning(true);

        configureButtonBindings();
    }

    /**
     * Use this method to define your button->command mappings. Buttons can be created by
     * instantiating a {@link GenericHID} or one of its subclasses ({@link
     * edu.wpi.first.wpilibj.Joystick} or {@link XboxController}), and then passing it to a {@c
     * edu.wpi.first.wpilibj2.command.button.JoystickButton}.
     */
    private void configureButtonBindings() {
        /* Driver Buttons */
        kX.onTrue(new InstantCommand(() -> s_Swerve.zeroGyro()));
        
        kY.onTrue(new InstantCommand(() -> s_Intake.setPower(0.9)));
        kY.onFalse(new InstantCommand(() -> s_Intake.stop()));
        
        // Pose2d target = new Pose2d(new Translation2d(1, 0), Rotation2d.fromDegrees(45));
        // Translation2d single = target.getTranslation().rotateBy(target.getRotation().unaryMinus());
        
        // Command initiateDriveToAprilTag = 
        // kB.onTrue(new InstantCommand(() -> {
        //     if (!testCommandIsRunning) {
        //         testCommand = new AlignWithAprilTag(s_Swerve, s_Vision)
        //             .andThen(new PrimeAndShoot(s_Shooter, s_Intake, 1.0))
        //             .finallyDo(() -> testCommandIsRunning = false)
        //             .withName("AlignWithAprilTag");

        //         testCommand.schedule();
        //         testCommandIsRunning = true;
        //     }
        // }).withName("InitiateDriveToAprilTag"));

        // kB.onTrue(new InstantCommand(() -> {
        //     if (!testCommandIsRunning) {
        //         testCommand = new GetAprilTagPose(s_Vision)
        //             .andThen(new VisionDrive(s_Swerve, s_Vision))
        //             .andThen(new VisionSpin(s_Swerve, s_Vision))
        //             .andThen(new PrimeAndShoot(s_Shooter, s_Intake, 1.0))
        //             .andThen(new InstantCommand(() -> s_Vision.resetPose()))
        //             .andThen(new PrintCommand("Finished"))
        //             .finallyDo(() -> testCommandIsRunning = false);

        //         testCommand.schedule();
        //         testCommandIsRunning = true;
        //     }
        // }));

        // kB.onTrue(new InstantCommand(() -> {
        //     if (!testCommandIsRunning) {
        //         if (!s_Vision.getAprilTag().isEmpty()) {
        //             s_Swerve.off();
        //             AlignWithAprilTag alignWithAprilTag = new AlignWithAprilTag(s_Swerve, s_Vision);
        //             testCommand = alignWithAprilTag
        //                 .alongWith(new PrimeWhileThenShoot(s_Shooter, s_Intake, 1, () -> !alignWithAprilTag.isRunning()))
        //                 .finallyDo(() -> testCommandIsRunning = false)
        //                 .withName("AlignWhilePriming");

        //             testCommand.schedule();
        //             testCommandIsRunning = true;
        //         }
        //     }
        // }).withName("InitiateAlignWithAprilTag"));

        // kB.whileTrue(new InstantCommand(() -> {
        //     if (!testCommandIsRunning && s_Vision.getAprilTag().isPresent()) {
        //         s_Swerve.off();
        //         AlignWithAprilTag alignWithAprilTag = new AlignWithAprilTag(s_Swerve, s_Vision);
        //         testCommand = alignWithAprilTag
        //             .alongWith(new PrimeWhileThenShoot(s_Shooter, s_Intake, 1, () -> !alignWithAprilTag.isRunning()))
        //             .finallyDo(() -> testCommandIsRunning = false)
        //             .withName("AlignWhilePriming");
                
        //         testCommand.schedule();
        //         testCommandIsRunning = true;
        //     }
        // }).withName("InitiateAlignWithAprilTag").repeatedly());

        // kA.onTrue(new InstantCommand(() -> {
        //     if (testCommand != null) {
        //         testCommand.cancel();
        //         testCommandIsRunning = false;
        //     }
        //     testCommand = null;
        // }).withName("Cancel Test Command"));

        kA.onTrue(new Spin(s_Swerve, () -> new Pose2d(0, 0, new Rotation2d(90))));

        kB.onTrue(new Drive(s_Swerve, () -> new Pose2d(1, 0, new Rotation2d(0))));

        rightBumper.onTrue(new IntakeAndHold(s_Intake, s_Shooter, () -> rightBumper.getAsBoolean()));
        leftBumper.onTrue(new InstantCommand(() -> s_Intake.eject()));
        leftBumper.onFalse(new InstantCommand(() -> s_Intake.stop()));

        DPadUp.onTrue(new InstantCommand(() -> s_Climber.setPower(1.0)));
        DPadUp.onFalse(new InstantCommand(() -> s_Climber.setPower(0)));
        DPadDown.onTrue(new InstantCommand(() -> s_Climber.setPower(-0.8)));
        DPadDown.onFalse(new InstantCommand(() -> s_Climber.setPower(0)));

        DPadLeft.onTrue(new PrimeAndShoot(s_Shooter, s_Intake, 1.0));
        // DPadLeft.onTrue(new InstantCommand(() -> s_Shooter.setAmpPower(-0.4)));
        // DPadLeft.onFalse(new InstantCommand(() -> s_Shooter.setAmpPower(0)));
        // DPadRight.onTrue(new InstantCommand(() -> s_Shooter.setAmpPower(0.4)));
        // DPadRight.onFalse(new InstantCommand(() -> s_Shooter.setAmpPower(0)));

        DPadUp2.onTrue(new InstantCommand(() -> s_Climber.setLeftPower(0.4)));
        DPadUp2.onFalse(new InstantCommand(() -> s_Climber.setLeftPower(0)));
        DPadDown2.onTrue(new InstantCommand(() -> s_Climber.setLeftPower(-0.4)));
        DPadDown2.onFalse(new InstantCommand(() -> s_Climber.setLeftPower(0)));

        kY2.onTrue(new InstantCommand(() -> s_Climber.setRightPower(0.4)));
        kY2.onFalse(new InstantCommand(() -> s_Climber.setRightPower(0)));
        kA2.onTrue(new InstantCommand(() -> s_Climber.setRightPower(-0.4)));
        kA2.onFalse(new InstantCommand(() -> s_Climber.setRightPower(0)));
        kB2.onTrue(new InstantCommand(() -> System.out.println("Auto-Align Failed. YIPPEEEEEEE!!!!")));
    }

    /**
     * Use this to pass the autonomous command to the main {@link Robot} class.
     *
     * @return the command to run in autonomous
     */
    public Command getAutonomousCommand() {
        // return new Drive(s_Swerve, () -> new Pose2d(new Translation2d(-0.94, Rotation2d.fromDegrees(-45)), new Rotation2d()))
        //     .andThen(new PrimeAndShoot(s_Shooter, s_Intake, 1.0))
        //     .andThen(new Drive(s_Swerve, () -> new Pose2d(new Translation2d(-2, 0), new Rotation2d())));

        // return new Drive(s_Swerve, () -> new Pose2d(new Translation2d(-2, 0), new Rotation2d()));
            
        // return new Drive(s_Swerve, () -> new Pose2d(-1, 0, new Rotation2d()))
        //     .andThen(new GetAprilTagPose(s_Vision))
        //     .andThen(new VisionDrive(s_Swerve, s_Vision))
        //     .andThen(new VisionSpin(s_Swerve, s_Vision))
        //     .andThen(new PrimeAndShoot(s_Shooter, s_Intake, 1.0))
        //     .andThen(new InstantCommand(() -> s_Vision.resetPose()))
        //     .andThen(new PrintCommand("Finished"));

        // return new Command() {};

        return new PathPlannerAuto("Amp-Side Two Piece");
    }
}
