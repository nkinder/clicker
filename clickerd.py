from SimpleXMLRPCServer import SimpleXMLRPCServer
from SimpleXMLRPCServer import SimpleXMLRPCRequestHandler
import argparse
import daemon
import fcntl
import grp
import logging
import logging.config
import os
import pwd
import signal
import socket
import sys
import syslog
import threading
import ConfigParser
import remote


# Shutdown signal handler
def shutdown_handler(signum, frame):
    global server

    logger.info('Received shutdown signal.')
    server.shutdown()


# NGK - from http://code.activestate.com/recipes/577911-context-manager-for-a-daemon-pid-file/
# This class is available under the MIT license.
class PidFile(object):
    """Context manager that locks a pid file.  Implemented as class
    not generator because daemon.py is calling .__exit__() with no parameters
    instead of the None, None, None specified by PEP-343."""
    # pylint: disable=R0903

    def __init__(self, path):
        self.path = path
        self.pidfile = None

    def __enter__(self):
        self.pidfile = open(self.path, "a+")
        try:
            fcntl.flock(self.pidfile.fileno(), fcntl.LOCK_EX | fcntl.LOCK_NB)
        except IOError:
            raise SystemExit("Already running according to " + self.path)
        self.pidfile.seek(0)
        self.pidfile.truncate()
        self.pidfile.write(str(os.getpid()))
        self.pidfile.flush()
        self.pidfile.seek(0)
        return self.pidfile

    def __exit__(self, exc_type=None, exc_value=None, exc_tb=None):
        try:
            self.pidfile.close()
        except IOError as err:
            # ok if file was just closed elsewhere
            if err.errno != 9:
                raise
        os.remove(self.path)


# Get our command-line args.
parser = argparse.ArgumentParser()
parser.add_argument("config", help="full path of the server config file")
parser.add_argument("--debug", help="enable debug logging", action="store_true")
args = parser.parse_args()


# Load server config file.
configfile = args.config
try:
    f = open(configfile, 'r')
except IOError as e:
    sys.stderr.write('Error opening the config file:\n')
    sys.stderr.write('{0}\n'.format(e))
    sys.exit(1)

config = ConfigParser.RawConfigParser()
config.read(configfile)

# Get the configured pidfile path.
try:
    pidfile=config.get('main', 'pidfile')
except ConfigParser.Error:
    sys.stderr.write('Configuration error:\n')
    sys.stderr.write('The pidfile setting must be specified in the ' +
                     '[main] section of {0}.\n'.format(configfile))
    sys.exit(1)

# Set the user and group to run as.  We default
# to the current user and group.
user = os.getuid()
group = os.getgid()

# See if we need to override the default user 
# and group from the config file.
if config.has_option('main', 'user'):
    username = config.get('main', 'user')
    try:
        user = pwd.getpwnam(username)[2]
    except KeyError:
        sys.stderr.write('Configuration error:\n')
        sys.stderr.write('The specified user does not exist ({0}).\n'.format(username))
        sys.exit(1)

if config.has_option('main', 'group'):
    groupname = config.get('main', 'group')
    try:
        group = grp.getgrnam(groupname)[2]
    except KeyError:
        sys.stderr.write('Configuration error:\n')
        sys.stderr.write('The specified group does not exist ({0}).\n'.format(groupname))
        sys.exit(1)

# Set up the daemon context.  If debug logging is enabled, redirect
# stderr and stdout to the debug log instead of /dev/null.
if args.debug:
    debugfile = '/tmp/clickerd.dbg'
    sys.stderr.write('Logging debug information to {0}.\n'.format(debugfile))
    f = open(debugfile, 'a')
    daemon = daemon.DaemonContext(uid=user, gid=group, umask=18,
                                  pidfile=PidFile(pidfile),
                                  stdout=f, stderr=f)
else:
    daemon = daemon.DaemonContext(uid=user, gid=group, umask=18,
                                  pidfile=PidFile(pidfile))

# Daemonize.
try:
    daemon.open()
except SystemExit as e:
    syslog.syslog('Error starting clicker daemon:')
    syslog.syslog('{0}'.format(e))
    raise

# Configure and begin logging.
logging.config.fileConfig(configfile)
logger = logging.getLogger('remoteServer')

# Start the XMLRPC server
logger.info('----- Starting Up -----')
# Restrict to a particular path.
class RequestHandler(SimpleXMLRPCRequestHandler):
    rpc_paths = ('/RPC2',)

try:
    server = SimpleXMLRPCServer((config.get('main', 'listenhost'),
                                 config.getint('main', 'listenport')),
                                RequestHandler, False)
except socket.error as e:
    logging.error('Unable to bind to {0}:{1}'.format(config.get('main', 'listenhost'),
                                                     config.getint('main', 'listenport')))
    logging.error(e)
    exit(1)

# Load controllable devices
logger.info('----- Loading Devices -----')
try:
    devices = remote.load_devices(config.get('main', 'devicedir'))
except:
    logger.error('Unable to load device configuration.');
    logger.error('----- Shutting down -----');
    exit(1)

# Load activities
logger.info('----- Loading Activities -----')
try:
    activities = remote.load_activities(config.get('main', 'activitydir'))
except:
    logger.error('Unable to load activity configuration.');
    logger.error('----- Shutting down -----')
    exit(1)

# Register our RPC calls.
logger.info('----- Registering RPC Calls -----')
server.register_function(remote.device_list)
server.register_function(remote.device_info)
server.register_function(remote.device_list_buttons)
server.register_function(remote.device_press_button)
server.register_function(remote.activity_list)
server.register_function(remote.activity_info)
server.register_function(remote.activity_start)
server.register_function(remote.activity_current)
server.register_function(remote.power_off)

# Run the server's main loop in a thread
logger.info('----- Starting Server -----')
server_thread = threading.Thread(target=server.serve_forever)
server_thread.daemon = True
server_thread.start()
logger.info('Listening on {0}:{1}'.format(config.get('main', 'listenhost'),
                                          config.getint('main', 'listenport')))
logger.info('----- Server Started -----')

# Wait here for the shutdown signal.
signal.signal(signal.SIGTERM, shutdown_handler)
signal.pause()

logger.info('----- Server Stopped -----')
exit(0)


