from SimpleXMLRPCServer import SimpleXMLRPCServer
from SimpleXMLRPCServer import SimpleXMLRPCRequestHandler
import logging
import logging.config
import ConfigParser
import remote

# NGK - don't hardcode path!  Pass it in instead, or set via an
# environment variable.
configfile = './config/server.conf'

# Configure and begin logging.
logging.config.fileConfig(configfile)
logger = logging.getLogger('remoteServer')

# Load config
config = ConfigParser.RawConfigParser()
config.read(configfile)

# Load controllable devices
logger.info('----- Loading Devices -----')
try:
    devices = remote.load_devices(config.get('main', 'devicedir'))
except:
    logger.error('Unable to load device configuration.');
    raise

# Load activities
logger.info('----- Loading Activities -----')
try:
    activities = remote.load_activities(config.get('main', 'activitydir'))
except:
    logger.error('Unable to load activity configuration.');
    raise

# Start the server.
logger.info('----- Starting Server -----')

# Restrict to a particular path.
class RequestHandler(SimpleXMLRPCRequestHandler):
    rpc_paths = ('/RPC2',)

# Create server
try:
    server = SimpleXMLRPCServer((config.get('main', 'listenhost'),
                                 config.getint('main', 'listenport')),
                                RequestHandler, False)
except:
    logger.error('Unable to start server.');
    exit

logger.info('Listening on {0}:{1}'.format(config.get('main', 'listenhost'),
                                          config.getint('main', 'listenport')))

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

# Run the server's main loop
# NGK - change this so we can do a graceful stop when we get a SIGTERM
logger.info('----- Waiting for requests -----')
server.serve_forever()

