LMAX Collections
==============

A High Performance Collections Library

Maintainer
==========

Coalescing Ring Buffer:
[Nick Zeeb] (https://github.com/nickzeeb)

What is it?
==========

Please see http://nickzeeb.wordpress.com/2013/03/07/the-coalescing-ring-buffer/ for an introduction

Ports:
==========
* .Net https://github.com/ncornwell/NCoalescingRingBuffer
* Python https://github.com/jstasiak/coalringbuf

Changelog
==========

## 1.0.0 Released (7-Mar-2013)

- Initial release containing the Coalescing Ring Buffer

## 1.1.0 Released (16-Jun-2013)

- Coalescing Ring Buffer improvements:
    - 4% performance improvement by using lazy sets where possible
    - constructor now rounds up capacity to the nearest higher power of two instead of throwing an exception
    - nextRead index has been renamed firstWrite for clarity

## 1.1.3 Released (7-Nov-2014)

- Coalescing Ring Buffer improvements:
    - improve size method to return a consistent result when reading two volatiles
    - see http://psy-lob-saw.blogspot.co.uk/2014/07/concurrent-bugs-size-matters.html for details
    - thanks to Stanimir Simeonoff, Nitsan Wakart and Martin Thompson!

## 1.1.3 Released (5-Jul-2017)

- Coalescing Ring Buffer improvements:
    - Null out entries in the ring buffer once consumed
