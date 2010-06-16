---
layout: default
title: Ilk Concerns and Decisions
---

# Size

My primary concern is the size of Ilk. It turns out that, Ilk itself cannot be
as small as I would like. Ideally, it would be a tiny library, under 5k, but
once you add the bulk that comes from having to define implementations for
ParametrizedType and WildcardType, it is hard to come back down to a reasonable
size. It doesn't make sense to split the `Ilk.Box` out of `Ilk`. There is not
much room for savings, so employing `Ilk` has a 20k cost.

It does a lot, however. Which is why Ilk Inject is only 93k with all
dependencies.

Yet, there are applications like Stash, where things like assignability are not
needed, only equality. (But it used anyway, since Stash uses boxes, but we could
just follow the turnstile rule. Hmmm... What is that? That is, we have a
turnstyle, so we know what goes in and what comes out.)
