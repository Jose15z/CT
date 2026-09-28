package com.culitostracker.domain.model;

/**
 * Entitlements. FREE keeps the core (partners, cycle, check-ins, advice,
 * agenda, XP, invites, export); PRO unlocks long trends, push reminders and
 * an unlimited gift list. When billing is not configured, everyone is PRO.
 */
public enum Plan {
    FREE, PRO;

    public static final int FREE_TRENDS_WEEKS = 4;
    public static final int FREE_WISHLIST_ITEMS = 5;
}
