#!/usr/bin/env python

from distutils.core import setup

setup(name='clicker',
      version='0.1',
      description='remote control server',
      author='Nathan Kinder',
      author_email='nkinder@redhat.com',
      url='http://github.com/nkinder/clicker',
      license='GPLv3+',
      package_dir = {'clicker': 'lib'},
      packages=['clicker'],
      data_files=[('/etc/clicker', ['config/server.cfg']),
                  ('/usr/sbin', ['server/clickerd']),
                  ('/usr/lib/systemd/system', ['server/systemd/clickerd.service']),
                  ('/etc/tmpfiles.d', ['server/systemd/clickerd.conf'])]
     )
