# Authors: Nathan Kinder <nkinder@redhat.com>
#
# Copyright (C) 2012  Nathan Kinder
# see file 'COPYING' for use and warranty information
#
# This program is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.
#

import io
import os
import re
import logging
import serial
import urllib2
import ConfigParser
import codecs
import binascii

# Fetch our logger
logger = logging.getLogger('clickerLogger')

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
def device_list_status_cmds(device):
    if device in devices:
        return devices[device].list_status_cmds()
    else:
        return []

def device_get_status(device, command):
    retval = ''

    if device in devices:
        retval = devices[device].get_status(command)

    return retval

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

        # Load the status commands and parsing info.
        self.status_cmds = {}
        if self.config.has_section('status_cmds'):
            for cmd in self.config.items('status_cmds'):
                try:
                    self.status_cmds[cmd[0]] = StatusCommand(cmd[0], *(cmd[1].split(',')))
                except Exception as e:
                    logger.debug('Error loading status command ({0}):'.format(cmd[0]))
                    logger.debug('{0}'.format(e))
                    raise

        # See if any response values are defined for any of the status commands.
        for cmd in self.status_cmds.keys():
            if self.config.has_section('{0}_values'.format(cmd)):
                self.status_cmds[cmd].set_responses(self.config.items('{0}_values'.format(cmd)))

        # Parse communication mode options.
        if self.mode == 'rs232':
            self.mode = MODE_RS232

            # Set the defaults.
            self.terminal = '/dev/ttyS0'
            self.baud = 9600
            self.bytesize = serial.EIGHTBITS
            self.stopbits = serial.STOPBITS_ONE
            self.parity = serial.PARITY_NONE
            self.timeout = 1

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

                if self.config.has_option('rs232', 'timeout'):
                    self.timeout = self.config.getint('rs232', 'timeout')

            # Open the connection to this device.
            try:
                self.connection = serial.Serial(self.terminal, self.baud, self.bytesize, self.parity,
                                                self.stopbits, self.timeout)
                self.sio = io.TextIOWrapper(io.BufferedRWPair(self.connection, self.connection))
            except serial.SerialException as e:
                logger.warning('Unable to open rs232 connection to device {0}'.format(self.name))
                logger.warning('{0}'.format(e))
                self.connection = None
                self.sio = None

        elif self.mode == 'http':
            self.mode = MODE_HTTP

            # Set the defaults.
            self.host = '127.0.0.1'
            self.port = 8000
            self.timeout = 5

            # See if the config file overrides any defaults.
            if self.config.has_section('http'):
                if self.config.has_option('http', 'host'):
                    self.host = self.config.get('http', 'host')

                if self.config.has_option('http', 'port'):
                    self.port = self.config.get('http', 'port')

                if self.config.has_option('http', 'timeout'):
                    self.timeout = self.config.getint('http', 'timeout')

            # Build the URL for each button command and store it
            # in the mapping.  This is more efficient than building
            # a URL every time a button is pressed.
            for button in self.buttons:
                self.buttons[button] = 'http://{0}:{1}/{2}'.format(self.host, self.port, self.buttons[button])

        else:
            raise ConfigError('Unknown mode.  Valid modes are rs232 and http.');

    def reopen_connection(self):
        if self.mode == MODE_RS232:
            logger.debug('Reopening connection...')
            try:
                self.connection = serial.Serial(self.terminal, self.baud, self.bytesize, self.parity,
                                                self.stopbits, self.timeout)
                self.sio = io.TextIOWrapper(io.BufferedRWPair(self.connection, self.connection))
            except serial.SerialException as e:
                logger.warning('Unable to open rs232 connection to device {0}'.format(self.name))
                logger.warning('{0}'.format(e))
                self.connection = None
                self.sio = None
                return 1

            return 0
        else:
            # The http mode doesn't need the connection opened until we request a URL.
            return 0

    def list_status_cmds(self):
        return sorted(self.status_cmds.keys())

    def get_status(self, command):
        logger.info('Received status request ({0}, {1})'.format(self.name, command))

        if command in self.status_cmds:
            # See what communication mode is used for this device and send the
            # command accordingly.
            if self.mode == MODE_RS232:
                if not self.connection:
                    if self.reopen_connection() != 0:
                        return ''

                logger.debug('Sending {0} command to device {1} ({2}, {3}, {4}, {5}, {6})'.
                             format(codecs.escape_encode(self.status_cmds[command].command)[0], self.name,
                                    self.terminal, self.baud, self.bytesize, self.stopbits, self.parity))

                try:
                    self.connection.write(self.status_cmds[command].command)
                except serial.SerialException as e:
                    logger.error('Unable to write to rs232 connection for device {0}'.format(self.name))
                    logger.error('{0}'.format(e))
                    return ''

                try:
                    if (self.status_cmds[command].response_size == 0):
                        self.connection.flushInput()
                        # Use serial io for universal readline support
                        response = self.sio.readline()
                    else:
                        response = self.connection.read(size=self.status_cmds[command].response_size)
                except serial.SerialException as e:
                    logger.error('Unable to read response from device {0}'.format(self.name))
                    logger.error('{0}'.format(e))
                    return ''

                logger.debug('----- Received Response -----')
                logger.debug('0x{0}'.format(binascii.hexlify(response)))

                # Parse the result to find out what value we should return.
                retval = self.status_cmds[command].get_result(response)
                logger.debug('Returning value ({0})'.format(retval))

                return retval
            elif self.mode == MODE_HTTP:
                logger.debug('Sending {0} to device {1}'.format(self.status_cmds[command], self.name))

                try:
                    f = urllib2.urlopen(url=self.status_cmds[command], timeout=self.timeout)

                    # NGK TODO - responses from http are going to be very different from rs232.
                    # Need to determine how we want to set up response config and deal with
                    # parsing for http.
                    logger.debug('----- Received Response -----')
                    logger.debug(f.read())
                except urllib2.URLError as e:
                    # We could have sent the request, but just not received the response back
                    # before the timeout.  We just log an error on the server side and return
                    # an empty response to the client.
                    logger.error('Error accessing URL:')
                    logger.error(e.reason);

                # NGK TODO - return actual response here after implementing http response handling.
                return ''
            else:
                logger.warning('Unknown communication mode in get_status ({0}, {1})'.format(self.name, self.mode))
                return ''
        else:
            logger.warning('Received request for non-existent status command ({0}, {1})'.format(self.name, command))
            return ''

    def list_buttons(self):
        return sorted(self.buttons.keys())

    def press_button(self, button):
        logger.info('Received button press ({0}, {1})'.format(self.name, button))

        if button in self.buttons:
            # See what communication mode is used for this device and send the
            # command accordingly.
            if self.mode == MODE_RS232:
                if not self.connection:
                    if self.reopen_connection() != 0:
                        return 1

                logger.debug('Sending {0} command to device {1} ({2}, {3}, {4}, {5}, {6})'.
                             format(codecs.escape_encode(self.buttons[button])[0], self.name,
                                    self.terminal, self.baud, self.bytesize, self.stopbits, self.parity))

                try:
                    self.connection.write(self.buttons[button])
                except serial.SerialException as e:
                    logger.error('Unable to write to rs232 connection for device {0}'.format(self.name))
                    logger.error('{0}'.format(e))
                    return 1

                return 0
            elif self.mode == MODE_HTTP:
                logger.debug('Sending {0} to device {1}'.format(self.buttons[button], self.name))

                try:
                    f = urllib2.urlopen(url=self.buttons[button], timeout=self.timeout)

                    logger.debug('----- Received Response -----')
                    logger.debug(f.read())
                except urllib2.URLError as e:
                    # We could have sent the request, but just not received the response back
                    # before the timeout.  We just log an error on the server side but return
                    # success to the client, as the action may have taken place.  This is really
                    # a server side error anyway, and there is nothing that the client can do
                    # about it.
                    logger.error('Error accessing URL:')
                    logger.error(e.reason);

                return 0
            else:
                logger.warning('Unknown communication mode in press_button ({0}, {1})'.format(self.name, self.mode))
                return 1
        else:
            logger.warning('Received request for non-existent button ({0}, {1})'.format(self.name, button))
            return 1


class StatusCommand:
    """Represents a device status command and it's possible responses"""

    def __init__(self, name, command, response_size, byte_filter, bit_filter, response_type):
        self.name = name
        # Decode any escaped hex chars that need to be sent to the device.
        self.command = codecs.escape_decode(command)[0]
        self.response_size = int(response_size)
        self.byte_filter = byte_filter
        self.bit_filter = bit_filter
        self.response_type = response_type

        # Initialize the response list
        self.responses = []

    def set_responses(self, responses):
        self.responses = responses

    def get_result(self, response):
        # If the response is not the right size, just pretend the response was empty.
        if (self.response_size != 0) and (len(response) != self.response_size):
            logger.warning('Incorrect response size (got {0}, expected {1})'.format(len(response),
                                                                                    self.response_size))
            logger.warning('Ignoring response value.')
            value = ''
        else:
            # Trim the bytes of interest.
            trimmed = eval('response[' + self.byte_filter + ']')
            logger.debug('----- Byte Trimmed Response -----')
            logger.debug('0x{0}'.format(binascii.hexlify(trimmed)))

            # Convert to requested type.  If the type isn't int, assume it's just a string.
            if self.response_type == 'int':
                # This takes our binary string data that was byte trimmed, hex encodes it,
                # then converts it to an integer.  We need an integer so we can perform
                # the bitwise operation specified in bit_filter.  After applying the
                # bit filter, we then convert the result to a numeric string that can
                # be returned to the client, or used in the regex below.
                value = str(eval('int(binascii.hexlify(trimmed), 16)' + self.bit_filter))
            else:
                # The trimmed value is already a string.
                value = trimmed

        # If responses list is empty, we return the whole trimmed response (converted for type).
        if len(self.responses) != 0:
            # See which regex matches the trimmed response and return the appropriate value.
            for resp in self.responses:
                logger.debug('Comparing ({0}) to regex ({1})'.format(value, resp[1]))
                if re.match(resp[1], value):
                    logger.debug('Found a match.  Returning ({0})'.format(resp[0]))
                    return resp[0]

            # If we made it here, no regex matched.  Return an empty string.
            logger.debug('No match found.')
            return ''
        else:
            return value


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
