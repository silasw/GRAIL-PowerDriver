To compile:
javac -cp "./lib/*" -d ./bin ./src/apps/SwitchController.java ./src/apps/Heater.java
javac -cp "./lib/*" -d ./bin ./src/driver/PowerDriver.java

To run:
java -cp "./bin:./lib/*" apps.Heater grail.rutgers.edu 7009 7010 winlab.chair.615 winlab.door.Yanyong
java -cp "./bin:./lib/*" driver.PowerDriver grail.rutgers.edu 7010


Note:
The required URI (winlab.powerswitch.heater) is hardcoded. Should probably change that.