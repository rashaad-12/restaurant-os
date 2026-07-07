package com.restaurantos.coresecurity.enums;

/**
 * The population a token is issued for. Carried in the standard {@code aud} JWT
 * claim and used to drive token lifetime and authorization scope — it answers
 * "who is this token for", independent of <em>how</em> the user authenticated.
 *
 * <ul>
 *   <li>{@code STAFF}    — first-party employees (internal password login)</li>
 *   <li>{@code PARTNER}  — delivery partners (OAuth login)</li>
 *   <li>{@code CUSTOMER} — end customers (OAuth login)</li>
 * </ul>
 */
public enum Audience {

    STAFF,

    PARTNER,

    CUSTOMER;

}
