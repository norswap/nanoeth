# Design Principles

This document outlines a few design principles for nanoeth.

## Goals / Trade-offs

**Goals**
- specify Ethereum behaviour in terms code and unit tests
- enable playing with example data interactively
- clear mapping with the Yellowpaper
- simplicity & clarity
- using a wealth of (wrapper) types to achieve the above objectives and make the codebase easy to
  navigate

**Non-Goals**
- efficiency

## Code Architecture

**Extensibility:** While the main goal is clarity, we want to make the codebase extensible so that
more efficient or flexible components can be swapped in. This should ideally not detract too much
from the simplicity and readability of the codebase.

At worse, we can supply very coarse-grained extensibility by using very high-level interface. This
kind of extensibility does not usually detract from readability — and may even improve it by
clearly identifying the API surface.

It's also possible to provide extensibility at multiple levels of granularity, so that there is a
choice to swap out smaller low-level components or big high-level components. Low-level
extensibility allows changing some details of the implementation without reinventing the wheel.
High-level extensibility allows reimplementing a component without being tied down by the
default architectue.

**Encapsulation:** Since one of the goal of nanoeth is to enable playing with the implementation,
we choose to expose a lot of things that would normally be considered implementation details and be
encapsulated (i.e. stay `private` or package-protected in Java). Encapsulation still has its uses
(e.g. hide helper function), but should in general not get in the way of learning about the system.

A good example is the `MemPatriciaNode#encode` method, which could have been kept package-protected
but is made public as a way to explore the implementation of the composition and cap functions (*c*
and *n*) defined the yellowpaper.