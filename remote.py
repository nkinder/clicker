import os
import re
import logging
import serial
import urllib2
import ConfigParser
import codecs

# Fetch our logger
logger = logging.getLogger('remoteServer')

# Loaded devices and activities
devices = {}
activities = {}

# Currently selected activity
current_activity = ''

# Device Communication Modes
MODE_RS232 = 1
MODE_HTTP = 2

# XML-RPC methods
def device_list():
    return sorted(devices.keys())

def device_info(device):
    if device in devices:
        return devices[device].description
    else:
        return ''

def device_list_buttons(device):
    if device in devices:
        return devices[device].list_buttons()
    else:
        return []

def device_press_button(device, button):
    rc = 1

    if device in devices:
        rc = devices[device].press_button(button)

    return rc

def activity_list():
    return sorted(activities.keys())

def activity_info(activity):
    if activity in activities:
        return activities[activity].description
    else:
        return ''

def activity_start(activity):
    rc = 1

    if activity in activities:
        rc = activities[activity].start()

    return rc

def activity_current():
    return current_activity

def power_off():
    global current_activity

    for device in devices.keys():
        devices[device].press_button('off')

    current_activity = ''

    return 0


# Helper methods
def load_devices(configdir):
    # Loop through device config files, validate, and create device objects.
    for file in os.listdir(configdir):
        # Filter out non .cfg files
        if re.match('.*\.cfg$', file):
            try:
                device = Device('{0}/{1}'.format(configdir, file))
                # Add device to device list
                devices[device.name] = device
            except ConfigError as e:
                logger.error('Unable to load device config ({0}):'.format(file))
                logger.error(e.msg)

def load_activities(configdir):
    # Loop through activity config files, validate, and create activity objects.
    for file in os.listdir(configdir):
        # Filter out non .cfg files
        if re.match('.*\.cfg$', file):
            try:
                activity = Activity('{0}/{1}'.format(configdir, file))
                # Add activity to activities list
                activities[activity.name] = activity
            except ConfigError as e:
                logger.error('Unable to load activity config ({0}):'.format(file))
                logger.error(e.msg)

# Custom Exceptions
class Error(Exception):
    """Base class for exceptions in this module."""
    pass

class ConfigError(Error):
    """Exception raised for errors in configuration.

    Attributes:
        expr -- input expression in which the error occurred
        msg  -- explanation of the error
    """

    def __init__(self, msg):
        self.msg = msg


class Device:
    """Represents a controllable device"""

    def __init__(self, configfile):
        logger.info('Parsing device config file ({0})'.format(configfile))

        # Read our config file.
        self.config = ConfigParser.RawConfigParser()
        self.config.read(configfile)

        # Load the device name and communication mode.  They are both required.
        try:
            self.name = self.config.get('general', 'name')
            self.mode = self.config.get('general', 'mode')
        except (ConfigParser.Error):
            raise ConfigError('Name and Mode must be specified in the [general] section of the device config file.')

        # If a description isn't in the config file, just use the name.
        if self.config.has_option('general', 'description'):
            self.description = self.config.get('general', 'description')
        else:
            self.description = self.name

        # Load the buttons to commands mapping.
        if self.config.has_section('buttons'):
            self.buttons = dict(self.config.items('buttons'))
        else:
            self.buttons = {}

        # We might have escaped hex in some of our commands, which need
        # need to be unescaped so we can send them to the devices.
        for button in self.buttons:
            self.buttons[button] = codecs.escape_decode(self.buttons[button])[0]

        # Parse communication mode options.
        if self.mode == 'rs232':
            self.mode = MODE_RS232

            # Set the defaults.
            self.terminal = '/dev/ttyS0'
            self.baud = 9600
            self.bytesize = serial.EIGHTBITS
            self.stopbits = serial.STOPBITS_ONE
            self.parity = serial.PARITY_NONE

            # See if the config file overrides any defaults.
            if self.config.has_section('rs232'):
                if self.config.has_option('rs232', 'terminal'):
                    self.terminal = self.config.get('rs232', 'terminal')

                if self.config.has_option('rs232', 'baud'):
                    self.baud = self.config.getint('rs232', 'baud')

                if self.config.has_option('rs232', 'bytesize'):
                    self.bytesize = self.config.getint('rs232', 'bytesize')
                    # Support values of 5, 6, 7, 8
                    if self.bytesize == 5:
                        self.bytesize = serial.FIVEBITS
                    elif self.bytesize == 6:
                        self.bytesize = serial.SIXBITS
                    elif self.bytesize == 7:
                        self.bytesize = serial.SEVENBITS
                    elif self.bytesize == 8:
                        self.bytesize = serial.EIGHTBITS
                    else:
                        raise ConfigError('Illegal rs232 bytesize specified.  Legal values are 5, 6, 7, 8.')

                if self.config.has_option('rs232', 'stopbits'):
                    self.stopbits = self.config.getint('rs232', 'stopbits')
                    # Support values of 1, 2
                    if self.stopbits == 1:
                        self.stopbits = serial.STOPBITS_ONE
                    elif self.stopbits == 2:
                        self.stopbits = serial.STOPBITS_TWO
                    else:
                        raise ConfigError('Illegal rs232 stopbits specified.  Legal values are 1, 2.')

                if self.config.has_option('rs232', 'parity'):
                    self.parity = self.config.get('rs232', 'parity')
                    # Support values none, even, odd
                    if self.parity.lower() == 'none':
                        self.parity = serial.PARITY_NONE
                    elif self.parity.lower() == 'even':
                        self.parity = serial.PARITY_EVEN
                    elif self.parity.lower() == 'odd':
                        self.parity = serial.PARITY_ODD
                    else:
                        raise ConfigError('Illegal rs232 parity specified.  Legal values are none, even, odd.')

            # Open the connection to this device.
            try:
                self.connection = serial.Serial(self.terminal, self.baud, self.bytesize, self.parity, self.stopbits, 1)
            except serial.SerialException:
                logger.warning('Unable to open rs232 connection to device {0}'.format(self.name))
                self.connection = None

        elif self.mode == 'http':
            self.mode = MODE_HTTP

            # Set the defaults.
            self.host = '127.0.0.1'
            self.port = 8000

            # See if the config file overrides any defaults.
            if self.config.has_section('http'):
                if self.config.has_option('http', 'host'):
                    self.host = self.config.get('http', 'host')

                if self.config.has_option('http', 'port'):
                    self.port = self.config.get('http', 'port')

            # Build the URL for each button command and store it
            # in the mapping.  This is more efficient than building
            # a URL every time a button is pressed.
            for button in self.buttons:
                self.buttons[button] = 'http://{0}:{1}/{2}'.format(self.host, self.port, self.buttons[button])

        else:
            raise ConfigError('Unknown mode.  Valid modes are rs232 and http.');

    def status(self):
        return 1

    def list_buttons(self):
        return sorted(self.buttons.keys())

    def press_button(self, button):
        # Assume failure.
        rc = 1

        logger.info('Received button press ({0}, {1})'.format(self.name, button))

        if button in self.buttons:
            # See what communication mode is used for this device and send the
            # command accordingly.
            if self.mode == MODE_RS232:
                logger.debug('Sending {0} command to device {1} ({2}, {3}, {4}, {5}, {6})'.
                            format(codecs.escape_encode(self.buttons[button])[0], self.name,
                                   self.terminal, self.baud, self.bytesize, self.stopbits, self.parity))

                if self.connection:
                    # NGK - send the command.
                    logger.debug('Have connection')
                else:
                    # NGK - do we need to do this check and open the connection?
                    logger.debug('No connection')

                rc = 0
            elif self.mode == MODE_HTTP:
                logger.debug('Sending {0} to device {1}'.format(self.buttons[button], self.name))

                try:
                    f = urllib2.urlopen(self.buttons[button])

                    logger.debug('----- Received Response -----')
                    logger.debug(f.read())
                except URLError as e:
                    logger.error('Error accessing URL:')
                    logger.error(e.reason);

                rc = 0
            else:
                logger.warning('Unknown communication mode in press_button ({0}, {1})'.format(self.name, self.mode))
                rc = 1
        else:
            logger.warning('Received request for non-existent button ({0}, {1})'.format(self.name, button))
            rc = 1

        return rc


class Activity:
    """Represents an end-user activity"""

    def __init__(self, configfile):
        logger.info('Parsing activity config file ({0})'.format(configfile))

        # Read our config file.
        self.config = ConfigParser.RawConfigParser()
        self.config.read(configfile)

        # Load the activity name.  It is required.
        try:
            self.name = self.config.get('general', 'name')
        except ConfigParser.Error:
            raise ConfigError('The name setting must be specified in the [general] section of the activity config file.')

        # If a description isn't in the config file, just use the name.
        if self.config.has_option('general', 'description'):
            self.description = self.config.get('general', 'description')
        else:
            self.description = self.name

        # Load the list of devices that need to be on for this activity.
        try:
            self.on_devices = self.config.get('general', 'on_devices').replace(' ','').split(',')
        except ConfigParser.Error:
            raise ConfigError('The on_devices setting must be specified in the [general] section of the activity config file.')

        # Ensure we know about all devices in the on devices list.
        for device in self.on_devices:
            if device not in devices:
                raise ConfigError('Unknown device in on_devices setting ({0}).'.format(device))

        # Load the list of device inputs that need to be selected for this activity.
        try:
            self.inputs = []
            for input in self.config.get('general', 'inputs').replace(' ','').split(','):
                input = input.split('.')
                self.inputs.append((input[0], input[1]))
        except:
            raise ConfigError('The inputs setting must be specified in the [general] section of the activity config file.') 

        # Ensure we know about all devices and buttons for input selection.
        for input in self.inputs:
            if input[0] not in devices:
                raise ConfigError('Unknown device in inputs setting ({0}).'.format(input[0]))
            elif input[1] not in devices[input[0]].buttons:
                raise ConfigError('Unknown button in inputs setting for device {0} ({1}).'.format(input[0], input[1]))

    def start(self):
        global current_activity

        logger.info('Starting activity {0}'.format(self.name))

        # Turn off all devices that are not needed for this
        # activity, and turn on the devices that are needed.
        for device in devices.keys():
            if device in self.on_devices:
                devices[device].press_button('on');
            else:
                devices[device].press_button('off')

        # Select inputs
        for input in self.inputs:
            devices[input[0]].press_button(input[1])

        # Set the current activity
        current_activity = self.name

        return 0
