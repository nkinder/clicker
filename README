clicker
=======

Remote control server/client for controlling home theater devices


The server will run as the user/group you start it as by default.  It
is recommended to create a system account for the server to run as.  The
user and group to run as can be specified in the server.cfg config file.

To do this, create a 'clicker' system user and group.  Ensure that this
user has permission to access the serial ports that are needed by your
devices.  On Fedora, you can do this by adding your user to the 'dialout'
group.  Here is a command that accomplishes this:

    useradd -r clicker -G dialout -d / -s /sbin/nologin

The default server.cfg file in the source tree has user and group settings
for 'clicker'.  Comment these lines out if you want to run clicker as the
user you start the process as.

The clicker user will need access to create and delete the pidfile that is
specified in server.cfg.  It is recommended to create a /var/run/clicker
directory that is owned by the clicker user/group, which should be used
to store the pidfile.

Your user will also need access to write your log file.  It is recommended
that you create a '/var/log/clicker' directory that is owned by your user
and group with permissions that allow you to create files within.  The log
file setting in server.xml should specify a file in that directory.

The user the server runs as only needs read access to the config files.  It
is recommended that the config files be owned by root with permissions of
644.
