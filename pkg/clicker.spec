%{!?pyver: %define pyver %(%{__python} -c "import sys ; print sys.version[:3]")}

Name:            clicker
Version:         0.1
Release:         1%{?dist}
Summary:         Remote Control server for RS232 and HTTP controllable devices

Group:           System Environment/Daemons
License:         GPLv3+
URL:             http://github.com/nkinder/clicker
Source0:         %{name}-%{version}.tar.gz

BuildArch:       noarch

# Build requirements
BuildRequires:   python-devel
BuildRequires:   python-daemon

# Scriptlet requirements
Requires(pre):   shadow-utils
Requires(post):  systemd-units
Requires(preun): systemd-units

# Runtime requirements
Requires:        python-daemon


%description
Clicker is a XMLRPC server for remote control of devices that have RS232
or HTTP command interfaces.  It allows easy creation of controllable
devices with virtual buttons that map to control commands.  These virtual
buttons can then be pressed via the XMLRPC interface.  Activities can
also be defined to allow common tasks that span multiple devices to be
run by a single XMLRPC call.

%prep
%setup -q


%build
%{__python} setup.py build


%install
%{__python} setup.py install -O1 --skip-build --root %{buildroot}

# Make activity and device config directories
mkdir -p $RPM_BUILD_ROOT%{_sysconfdir}/%{name}/activities
mkdir -p $RPM_BUILD_ROOT%{_sysconfdir}/%{name}/devices

# Make our pid and log directories
mkdir -p $RPM_BUILD_ROOT%{_localstatedir}/run/%{name}
mkdir -p $RPM_BUILD_ROOT%{_localstatedir}/log/%{name}


%pre
# If the clicker user and group don't exist, create them.
getent group clicker >/dev/null || groupadd -r clicker
getent passwd clicker >/dev/null || \
    useradd -r -g clicker -G dialout -d / -s /sbin/nologin \
    -c "Clicker remote control daemon user" clicker
exit 0


%post
/bin/systemctl daemon-reload > /dev/null 2>&1 || :
/bin/systemctl try-restart %{name}.service > /dev/null 2>&1 || :


%preun
if [ $1 -eq 0 ]; then
    # Final package removal, not upgrade.  Disable and stop the service.
    /bin/systemctl --no-reload disable %{name}d.service > /dev/null 2>&1 || :
    /bin/systemctl stop %{name}d.service > /dev/null 2>&1 || :
fi


%files
%defattr(-,root,root,-)
%doc COPYING README
%dir %{_sysconfdir}/%{name}
%config(noreplace)%{_sysconfdir}/%{name}/server.cfg
%dir %{_sysconfdir}/%{name}/activities
%dir %{_sysconfdir}/%{name}/devices
%{_unitdir}/%{name}d.service
%attr(755,clicker,clicker) %{_localstatedir}/log/%{name}
%attr(755,clicker,clicker) %{_localstatedir}/run/%{name} 
%attr(755,root,root) %{_sbindir}/%{name}d
%{python_sitelib}/%{name}/
%{python_sitelib}/%{name}-%{version}-py%{pyver}.egg-info/


%changelog
* Thu Aug 2 2012 Nathan Kinder <nkinder@redhat.com> 0.1-1
- Initial release

