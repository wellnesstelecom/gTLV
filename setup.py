#!/usr/bin/python

from setuptools import setup, find_packages

setup(name = "gtlv", version = '1.02', author = 'Daniel Carrion', author_email = 'dcarrion@wtelecom.es',
      description = 'Generic TLV transport protocol',
      long_description = open('README.txt').read(), zip_safe = True, packages = find_packages())
