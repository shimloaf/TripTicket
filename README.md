# TripTicket

A Minecraft Paper plugin for inviting players to temporarily visit another world.

When a host invites a guest, the guest's original location and world are saved. After the visit duration ends (default: 3 minutes), the guest is whisked back home after a 10-second warning.

---

## Commands

- `/ticket <player> [duration]` - Invite a player to visit your world.
- `/ticket accept` - Accept an invite.
- `/ticket deny` - Decline an invite.
- `/ticket return` - Return home early.
- `/ticket remaining` - Check time left on your visit.
- `/ticket cancel <player>` - Cancel an invite or visitor.
- `/ticket reload` - Reload config (`/ticket reload`).

*(Aliases: `/tripticket`, `/ticket`, `/trip`, `/tt`)*

---

## Administrator & Permissions Guide

### Default Permission Structure
Out of the box, TripTicket is pre-configured so that:
- **Everyone (all players)** can issue and accept trip tickets for the **default duration** (3 minutes), return home early, and check their remaining time.
- **Server Operators (Ops)** (or players with `tripticket.customduration`) can specify **any custom duration** when issuing a ticket (e.g. `/ticket Steve 10m` or `/ticket Steve 30s`). If a non-op specifies a duration, the plugin informs them and automatically applies the default duration.

---

### Configuring Permissions in `config.yml`

You do not need a permissions plugin to configure permissions. In `plugins/TripTicket/config.yml`, administrators can adjust permissions directly:

```yaml
permissions:
  # Allow all players to issue tickets for the default length (3m)
  allow-everyone-default-duration: true

  # Restrict custom durations (e.g., /ticket Steve 10m) to Ops only
  # Set to false to allow any player who can invite to pick the duration
  custom-duration-ops-only: true

  # Allow all players to accept incoming invitations
  allow-everyone-accept: true

  # Allow all players to return home early with /ticket return
  allow-everyone-return: true

  # Allow all players to check remaining time with /ticket remaining
  allow-everyone-check-time: true
```

---

### Using a Permissions Plugin (LuckPerms, etc.)

If you prefer to manage permissions through a permissions manager like **LuckPerms**, you can set `allow-everyone-default-duration: false` and assign permission nodes to your groups:

| Permission Node | Default | Description |
|---|---|---|
| `tripticket.invite` | `true` | Issue a trip ticket for the default duration. |
| `tripticket.customduration` | `op` | Specify a custom duration (e.g. `/ticket Steve 10m`). |
| `tripticket.use` | `true` | Accept incoming trip tickets (`/ticket accept`). |
| `tripticket.return` | `true` | Return home early (`/ticket return`). |
| `tripticket.time` | `true` | Check remaining visit time (`/ticket remaining`). |
| `tripticket.cooldown.bypass` | `op` | Bypass cooldown between issuing tickets. |
| `tripticket.warmup.bypass` | `op` | Bypass warmup delay before embarking. |
| `tripticket.reload` | `op` | Reload the configuration (`/ticket reload`). |
| `tripticket.admin` | `op` | Full admin privileges (cancel any visit, reload, bypass all). |

*Note: All permission node names can be customized in the `permissions.nodes` section of `config.yml`.*

---

## Building

```bash
mvn clean package
```
or
```bash
gradle build
```

Requires Java 21+.
