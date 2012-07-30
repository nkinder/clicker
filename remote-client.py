import xmlrpclib

s = xmlrpclib.ServerProxy('http://localhost:8000')

# Print list of available methods
#print s.system.listMethods()

# Print list of available devices
print s.list_devices()

# List available buttons for a device
print s.list_buttons('krell_hts')

# Press a button on a device
print s.press_button('krell_hts', 'on')
