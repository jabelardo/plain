plain-docs
==========

This module contains all documentation of plain. The documentation is created with reStructuredText and Sphinx.

See `reStructuredText Primer <http://sphinx.pocoo.org/rest.html#rst-primer>`_ or `Sphinx Markup Constructs <http://sphinx.pocoo.org/markup/index.html#sphinxmarkup>`_  for more information.

Installing Sphinx
-----------------

To use sphinx on your local (Unix based) machine follow the steps below:

* Make sure you have installed Python on your machine.

* If `Easy Install <http://peak.telecommunity.com/DevCenter/EasyInstall>`_ isn't already installed, download the appropriate setuptools-*.egg for your python version on http://pypi.python.org/pypi/setuptools#files.

* Execute the downloaded file ::

	sh  appropriate setuptools-*.egg

* Now download the appropriate Sphinx release on http://pypi.python.org/pypi/Sphinx and install Sphinx using ::

	easy_install <Your download path>/Sphinx-*egg

* Now you are able to use Sphinx.

Using Sphinx
------------

For general information have a look at http://sphinx.pocoo.org/tutorial.html.

On this project all source files are stored in the source directory. On Unix based systems you can use the Makefile to build the documentation. Just call ::

	make html

or ::

	make pdflatex

to create HTML or PDF [#f1]_.

.. [#f1] To create PDF you need an installed LaTeX environment.