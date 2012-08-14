import xmlrpclib

s = xmlrpclib.ServerProxy('http://localhost:8000/clicker')

# Print list of available methods
#print s.system.listMethods()

# Print list of available devices
print s.device_list()

# List available buttons for a device
print s.deveice_list_buttons('krell_hts')

# Press a button on a device
print s.device_press_button('krell_hts', 'on')
