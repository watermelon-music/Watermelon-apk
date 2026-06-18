const { Telegraf, Markup } = require('telegraf');
const { createClient } = require('@supabase/supabase-js');

const BOT_TOKEN = process.env.TELEGRAM_BOT_TOKEN || 'YOUR_BOT_TOKEN_HERE';
const ADMIN_CHAT_ID = process.env.TELEGRAM_ADMIN_CHAT_ID || 'YOUR_ADMIN_CHAT_ID';
const SUPABASE_URL = process.env.SUPABASE_URL || 'https://xljlceoircpibojirxob.supabase.co';
const SUPABASE_SERVICE_KEY = process.env.SUPABASE_SERVICE_KEY || 'YOUR_SERVICE_ROLE_KEY';

const bot = new Telegraf(BOT_TOKEN);
const supabase = createClient(SUPABASE_URL, SUPABASE_SERVICE_KEY, {
  auth: { autoRefreshToken: false, persistSession: false }
});

function isAdmin(ctx) {
  return String(ctx.chat?.id || ctx.from?.id) === String(ADMIN_CHAT_ID);
}

// Premium visual layout keyboard
const MAIN_KEYBOARD = Markup.keyboard([
  ['🍉 Dashboard', '👥 Users'],
  ['📊 Stats', '📅 Daily'],
  ['⏳ Pending', '🏆 Top Users'],
  ['🆕 Recent', '⚙️ Remote Config'],
  ['📢 Broadcast', '🔄 Refresh Home'],
  ['💎 Subs']
]).resize();

function b(text) { return `<b>${text}</b>`; }
function i(text) { return `<i>${text}</i>`; }
function c(text) { return `<code>${text}</code>`; }
function hr() { return '\n───────────────────\n'; }

// ========== HANDLER FUNCTIONS FOR REUSE ==========

async function handleStats(ctx) {
  if (!isAdmin(ctx)) return;
  try {
    const { count: users } = await supabase.from('profiles').select('*', { count: 'exact', head: true });
    const { count: free } = await supabase.from('profiles').select('*', { count: 'exact', head: true }).eq('plan', 'FREE');
    const { count: paid } = await supabase.from('profiles').select('*', { count: 'exact', head: true }).neq('plan', 'FREE');
    const { count: playlists } = await supabase.from('playlists').select('*', { count: 'exact', head: true });
    const { count: favorites } = await supabase.from('favorites').select('*', { count: 'exact', head: true });
    const { count: plays } = await supabase.from('listening_history').select('*', { count: 'exact', head: true });
    const { count: todayPlays } = await supabase.from('listening_history').select('*', { count: 'exact', head: true }).gte('played_at', new Date(Date.now() - 864e5).toISOString());
    ctx.reply(
      `🍉 ${b('Watermelon App Dashboard')}\n` +
      hr() +
      `👥 ${b('User Registry')}\n` +
      `• Total Accounts: ${b(users)}\n` +
      `• Free Tier: ${free}\n` +
      `• Premium Tier: ${paid}\n\n` +
      `📦 ${b('User Creations')}\n` +
      `• Total Playlists: ${b(playlists)}\n` +
      `• Total Liked Tracks: ${b(favorites)}\n\n` +
      `🎵 ${b('Streaming Stats')}\n` +
      `• Total Plays Streamed: ${b(plays)}\n` +
      `• Plays (Last 24h): ${b(todayPlays)}`,
      { parse_mode: 'HTML', ...MAIN_KEYBOARD }
    );
  } catch (e) {
    ctx.reply(`❌ Error: ${e.message}`, MAIN_KEYBOARD);
  }
}

async function handleUsers(ctx) {
  if (!isAdmin(ctx)) return;
  try {
    const { count: total, error: e1 } = await supabase.from('profiles').select('*', { count: 'exact', head: true });
    const { count: free, error: e2 } = await supabase.from('profiles').select('*', { count: 'exact', head: true }).eq('plan', 'FREE');
    const { count: paid, error: e3 } = await supabase.from('profiles').select('*', { count: 'exact', head: true }).neq('plan', 'FREE');
    if (e1 || e2 || e3) throw e1 || e2 || e3;
    
    ctx.reply(
      `👥 ${b('User Registry Summary')}\n` +
      hr() +
      `🟢 Total Users: ${b(total)}\n` +
      `⚪ Free Tier: ${b(free)}\n` +
      `💎 Premium Tier: ${b(paid)}\n\n` +
      `⚡ Paid Ratio: ${b(((paid/total)*100).toFixed(1))}%`,
      { parse_mode: 'HTML', ...MAIN_KEYBOARD }
    );
  } catch (e) {
    ctx.reply(`❌ Error: ${e.message}`, MAIN_KEYBOARD);
  }
}

async function handleSubs(ctx) {
  if (!isAdmin(ctx)) return;
  try {
    const { data, error } = await supabase.from('profiles')
      .select('email, plan, created_at')
      .neq('plan', 'FREE')
      .order('created_at', { ascending: false })
      .limit(15);
    if (error) throw error;
    
    let list = (data || []).map((u, index) => {
      return `${index + 1}. 💎 ${c(u.email)} - ${b(u.plan)} (${u.created_at?.slice(0,10)})`;
    }).join('\n');
    
    ctx.reply(
      `💎 ${b('Premium Subscribers (Latest)')}\n` +
      hr() +
      (list || '📭 No active premium subscriptions found.'),
      { parse_mode: 'HTML', ...MAIN_KEYBOARD }
    );
  } catch (e) {
    ctx.reply(`❌ Error: ${e.message}`, MAIN_KEYBOARD);
  }
}

async function handleDaily(ctx) {
  if (!isAdmin(ctx)) return;
  try {
    const yesterday = new Date(Date.now() - 864e5).toISOString();
    const { count: todayPlays } = await supabase.from('listening_history').select('*', { count: 'exact', head: true }).gte('played_at', yesterday);
    const { count: todaySignups } = await supabase.from('profiles').select('*', { count: 'exact', head: true }).gte('created_at', yesterday);
    const { data: topSong } = await supabase.from('listening_history').select('title, artist').gte('played_at', yesterday).limit(1).order('played_at', { ascending: false });
    const top = topSong?.[0] ? `${topSong[0].title} — ${topSong[0].artist || 'Unknown'}` : '📭 No plays yet';
    ctx.reply(
      `📅 ${b('Watermelon Daily Digest')} ${i('(24h)')}\n` +
      hr() +
      `🎵 Total Plays: ${b(todayPlays)}\n` +
      `🆕 New Signups: ${b(todaySignups)}\n` +
      `🔥 Top Song Played: ${i(top)}`,
      { parse_mode: 'HTML', ...MAIN_KEYBOARD }
    );
  } catch (e) {
    ctx.reply(`❌ Error: ${e.message}`, MAIN_KEYBOARD);
  }
}

async function handlePending(ctx) {
  if (!isAdmin(ctx)) return;
  try {
    const { data, error } = await supabase.from('premium_requests').select('*').eq('status', 'pending').order('created_at', { ascending: false }).limit(10);
    if (error) throw error;
    if (!data.length) return ctx.reply('⏳ No pending requests.', MAIN_KEYBOARD);

    for (const r of data) {
      const inlineKeyboard = Markup.inlineKeyboard([
        Markup.button.callback('✅ Approve', `approve_${r.id}`),
        Markup.button.callback('❌ Reject', `reject_${r.id}`)
      ]);
      await ctx.reply(
        `⏳ ${b('Pending Request')}\n\n` +
        `📧 Email: ${c(r.email)}\n` +
        `💎 Plan: ${b(r.plan)}\n` +
        `💰 Amount: ₹${r.amount / 100}\n` +
        `📅 Date: ${r.created_at?.slice(0, 10)}`,
        { parse_mode: 'HTML', ...inlineKeyboard }
      );
    }
  } catch (e) {
    ctx.reply(`❌ Error: ${e.message}`, MAIN_KEYBOARD);
  }
}

async function handleTopUsers(ctx) {
  if (!isAdmin(ctx)) return;
  try {
    const { data, error } = await supabase.from('listening_history').select('user_id, title').order('played_at', { ascending: false }).limit(500);
    if (error) throw error;
    const counts = {};
    data.forEach(d => { counts[d.user_id] = (counts[d.user_id] || 0) + 1; });
    const top = Object.entries(counts).sort((a, b) => b[1] - a[1]).slice(0, 10).map(([uid, c]) => ({ uid, count: c }));
    if (!top.length) return ctx.reply('📭 No plays yet.', MAIN_KEYBOARD);
    const ids = top.map(t => t.uid);
    const { data: users, error: ue } = await supabase.from('profiles').select('id, email, display_name').in('id', ids);
    if (ue) throw ue;
    const lines = top.map((t, i) => {
      const u = users?.find(u => u.id === t.uid);
      const name = u?.display_name || u?.email || t.uid.slice(0, 8);
      const medal = i === 0 ? '🥇' : i === 1 ? '🥈' : i === 2 ? '🥉' : '🏅';
      return `${medal} ${b(name)} — ${t.count} plays`;
    }).join('\n');
    ctx.reply(
      `🏆 ${b('Listener Leaderboard')}\n` +
      hr() +
      lines,
      { parse_mode: 'HTML', ...MAIN_KEYBOARD }
    );
  } catch (e) {
    ctx.reply(`❌ Error: ${e.message}`, MAIN_KEYBOARD);
  }
}

async function handleRecent(ctx) {
  if (!isAdmin(ctx)) return;
  try {
    const yesterday = new Date(Date.now() - 864e5).toISOString();
    const { data, error } = await supabase.from('profiles').select('email, display_name, created_at').gte('created_at', yesterday).order('created_at', { ascending: false }).limit(15);
    if (error) throw error;
    if (!data.length) return ctx.reply('📭 No new signups today.', MAIN_KEYBOARD);
    const lines = data.map((u, idx) => `${idx + 1}. 🆕 ${b(u.display_name || u.email)} (${u.created_at?.slice(11, 16)})`).join('\n');
    ctx.reply(
      `🆕 ${b('New Signups (Last 24h)')}\n` +
      hr() +
      lines,
      { parse_mode: 'HTML', ...MAIN_KEYBOARD }
    );
  } catch (e) {
    ctx.reply(`❌ Error: ${e.message}`, MAIN_KEYBOARD);
  }
}

async function sendOrEditConfig(ctx, config, edit = false) {
  const text = `⚙️ ${b('Watermelon App Remote Config')}\n` +
    hr() +
    `🛠️ Maintenance Mode: ${config.maintenance_mode ? '🚨 ' + b('ENABLED (BLOCKING)') : '🟢 ' + b('OFF (ACTIVE)')}\n` +
    `📺 YouTube Stream: ${config.disable_youtube ? '❌ ' + b('DISABLED') : '🟢 ' + b('ENABLED')}\n` +
    `🎧 Audius Stream: ${config.disable_audius ? '❌ ' + b('DISABLED') : '🟢 ' + b('ENABLED')}\n` +
    `🎵 Jamendo Stream: ${config.disable_jamendo ? '❌ ' + b('DISABLED') : '🟢 ' + b('ENABLED')}\n` +
    `📋 Max Free Playlists: ${b(config.free_max_playlists)}\n` +
    hr() +
    i('Use buttons below for one-tap changes, or commands like /disableall, /enableall');

  const inlineKeyboard = Markup.inlineKeyboard([
    [
      Markup.button.callback('🚨 Disable All', 'cfg_disable_all'),
      Markup.button.callback('🟢 Enable All', 'cfg_enable_all')
    ],
    [
      Markup.button.callback(config.maintenance_mode ? '🟢 Turn Maint OFF' : '🚨 Turn Maint ON', 'toggle_maint'),
      Markup.button.callback(config.disable_youtube ? '🟢 Enable YouTube' : '❌ Disable YouTube', 'toggle_yt')
    ],
    [
      Markup.button.callback(config.disable_audius ? '🟢 Enable Audius' : '❌ Disable Audius', 'toggle_aud'),
      Markup.button.callback(config.disable_jamendo ? '🟢 Enable Jamendo' : '❌ Disable Jamendo', 'toggle_jam')
    ]
  ]);

  if (edit) {
    try {
      await ctx.editMessageText(text, { parse_mode: 'HTML', ...inlineKeyboard });
    } catch (e) {
      // Message might be identical, ignore
    }
  } else {
    await ctx.reply(text, { parse_mode: 'HTML', ...inlineKeyboard });
  }
}

async function handleConfig(ctx) {
  if (!isAdmin(ctx)) return;
  try {
    let { data, error } = await supabase.from('remote_config').select('*').limit(1).single();
    
    if (error && error.code === 'PGRST116') {
      const defaultConfig = {
        maintenance_mode: false,
        disable_youtube: false,
        disable_audius: false,
        disable_jamendo: false,
        free_max_playlists: 3
      };
      const { data: inserted, error: initError } = await supabase.from('remote_config').insert(defaultConfig).select().single();
      if (!initError) data = inserted;
    }
    
    const config = data || {
      maintenance_mode: false,
      disable_youtube: false,
      disable_audius: false,
      disable_jamendo: false,
      free_max_playlists: 3
    };

    await sendOrEditConfig(ctx, config, false);
  } catch (e) {
    ctx.reply(
      `⚙️ ${b('Remote Config (Firebase Connected)')}\n` +
      hr() +
      `ℹ️ App uses Firebase Remote Config. To override with Telegram Remote Control, create a table named ` + c('remote_config') + ` in Supabase with columns:\n` +
      `• ` + c('maintenance_mode') + ` (boolean)\n` +
      `• ` + c('disable_youtube') + ` (boolean)\n` +
      `• ` + c('disable_audius') + ` (boolean)\n` +
      `• ` + c('disable_jamendo') + ` (boolean)\n` +
      `• ` + c('free_max_playlists') + ` (int4)`,
      { parse_mode: 'HTML', ...MAIN_KEYBOARD }
    );
  }
}

async function handleRefresh(ctx) {
  if (!isAdmin(ctx)) return;
  try {
    const { error } = await supabase.from('broadcasts').insert({
      message: '[REFRESH] Clear Room cache and reload home catalog',
      sender: 'Admin',
      active: true,
      created_at: new Date().toISOString()
    });
    if (error) throw error;
    ctx.reply(
      `🔄 ${b('Home Refresh Signal Sent!')}\n` +
      hr() +
      `All active apps will clear their database cache and fetch fresh trending songs.`,
      { parse_mode: 'HTML', ...MAIN_KEYBOARD }
    );
  } catch (e) {
    ctx.reply(`❌ Error: ${e.message}`, MAIN_KEYBOARD);
  }
}

// ========== REGISTER COMMANDS AND TEXT TRIGGERS ==========

bot.start((ctx) => {
  if (!isAdmin(ctx)) {
    return ctx.reply(
      '🍉 ' + b('Watermelon Music Controller') + '\n' +
      hr() +
      'This is the secure remote-control bot for the Watermelon App.\n\n' +
      '🔑 ' + b('Access Denied') + '\n' +
      'Please register your chat ID in the environment settings.\n\n' +
      '🆔 Your Chat ID: ' + c(ctx.chat?.id || ctx.from?.id) + '\n' +
      'Use /myid to copy it.',
      { parse_mode: 'HTML' }
    );
  }
  ctx.reply(
    '🍉 ' + b('Watermelon Admin Bot v2.0') + '\n' +
    hr() +
    '👋 ' + b('Welcome back, Chief!') + '\n' +
    'You now have global remote command over your Watermelon app fleet.\n\n' +
    '📱 Use the quick-keys below or type /commands for instructions.',
    { parse_mode: 'HTML', ...MAIN_KEYBOARD }
  );
});

bot.command('myid', (ctx) => {
  const id = ctx.chat?.id || ctx.from?.id;
  ctx.reply(
    '🆔 ' + b('Your Telegram Chat ID') + '\n\n' +
    c(id) + '\n\n' +
    '👉 Set this as ' + c('TELEGRAM_ADMIN_CHAT_ID') + ' to authorize this account.',
    { parse_mode: 'HTML' }
  );
});

bot.command('commands', (ctx) => {
  if (!isAdmin(ctx)) return;
  ctx.reply(
    '🍉 ' + b('Watermelon Remote Commands') + '\n' +
    hr() +
    '📊 ' + b('Fleet Stats & Insights') + '\n' +
    '• /stats — Global app analytics dashboard\n' +
    '• /users — Count of total, free, and premium users\n' +
    '• /subs — Recent premium subscriptions list\n' +
    '• /plays — Plays metrics and top trending songs\n' +
    '• /daily — Signup and activity digest (24h)\n' +
    '• /topusers — Leaderboard of most active listeners\n' +
    '• /recent — List of new signups (24h)\n' +
    '• /retention — Active users retention (7d/30d)\n\n' +
    '🛠️ ' + b('Remote Config Controls') + '\n' +
    '• /config — Current app remote configuration flags\n' +
    '• /maintenance &lt;on|off&gt; — Toggle global maintenance mode\n' +
    '• /disableall — One-tap disable all services / Maintenance ON\n' +
    '• /enableall — One-tap enable all services / Maintenance OFF\n' +
    '• /toggle_youtube &lt;on|off&gt; — Enable/disable YouTube streaming\n' +
    '• /toggle_audius &lt;on|off&gt; — Enable/disable Audius streaming\n' +
    '• /toggle_jamendo &lt;on|off&gt; — Enable/disable Jamendo streaming\n' +
    '• /set_playlists &lt;number&gt; — Set max playlists for free tier\n\n' +
    '🚨 ' + b('User & Communication Actions') + '\n' +
    '• /broadcast &lt;message&gt; — Send notification overlay to all users\n' +
    '• /refresh — Clear Room cache and reload home catalog\n' +
    '• /verify &lt;email&gt; — Manually grant premium subscription\n' +
    '• /revoke &lt;email&gt; — Revoke premium subscription\n' +
    '• /ban &lt;email&gt; — Ban a user from the application\n' +
    '• /unban &lt;email&gt; — Unban a user\n' +
    '• /pending — Manage waiting premium approvals',
    { parse_mode: 'HTML', ...MAIN_KEYBOARD }
  );
});

// Map dashboard buttons directly to the exact function handlers
bot.command('stats', handleStats);
bot.hears(['🍉 Dashboard', '📊 Stats'], handleStats);

bot.command('users', handleUsers);
bot.hears('👥 Users', handleUsers);

bot.command('subs', handleSubs);
bot.hears('💎 Subs', handleSubs);

bot.command('daily', handleDaily);
bot.hears('📅 Daily', handleDaily);

bot.command('pending', handlePending);
bot.hears('⏳ Pending', handlePending);

bot.command('topusers', handleTopUsers);
bot.hears('🏆 Top Users', handleTopUsers);

bot.command('recent', handleRecent);
bot.hears('🆕 Recent', handleRecent);

bot.command('config', handleConfig);
bot.hears('⚙️ Remote Config', handleConfig);

bot.command('refresh', handleRefresh);
bot.hears('🔄 Refresh Home', handleRefresh);

bot.hears('📢 Broadcast', (ctx) => {
  if (!isAdmin(ctx)) return;
  ctx.reply('📢 Usage: /broadcast [message] - Broadcast message to all active users.', MAIN_KEYBOARD);
});

bot.command('plays', async (ctx) => {
  if (!isAdmin(ctx)) return;
  try {
    const { count: totalPlays, error: e1 } = await supabase.from('listening_history').select('*', { count: 'exact', head: true });
    const { count: todayPlays, error: e2 } = await supabase.from('listening_history').select('*', { count: 'exact', head: true }).gte('played_at', new Date(Date.now() - 864e5).toISOString());
    const { data: topSongs, error: e3 } = await supabase.rpc('get_top_songs', { limit_n: 5 });
    if (e1 || e2) throw e1 || e2;
    
    let top = '';
    if (topSongs && topSongs.length) {
      top = '\n🔥 ' + b('Top Charting Songs:') + '\n' + topSongs.map((s, i) => `${i+1}. 🎵 ${b(s.title)} (${s.plays} plays)`).join('\n');
    }
    ctx.reply(
      `🎵 ${b('Media Streams Metrics')}\n` +
      hr() +
      `🎧 Cumulative Plays: ${b(totalPlays)}\n` +
      `📅 Last 24 Hours: ${b(todayPlays)}` +
      top,
      { parse_mode: 'HTML', ...MAIN_KEYBOARD }
    );
  } catch (e) {
    ctx.reply(`❌ Error: ${e.message}`, MAIN_KEYBOARD);
  }
});

bot.command('disableall', async (ctx) => {
  if (!isAdmin(ctx)) return;
  try {
    const { data, error } = await supabase
      .from('remote_config')
      .update({
        maintenance_mode: true,
        disable_youtube: true,
        disable_audius: true,
        disable_jamendo: true
      })
      .neq('id', 0)
      .select()
      .single();
    if (error) throw error;
    ctx.reply(`🚨 ${b('Disable All Executed!')}\n\n• Maintenance: ${b('ON')}\n• YouTube: ${b('DISABLED')}\n• Audius: ${b('DISABLED')}\n• Jamendo: ${b('DISABLED')}`, { parse_mode: 'HTML', ...MAIN_KEYBOARD });
  } catch (e) {
    ctx.reply(`❌ Database Error: ${e.message}`);
  }
});

bot.command('enableall', async (ctx) => {
  if (!isAdmin(ctx)) return;
  try {
    const { data, error } = await supabase
      .from('remote_config')
      .update({
        maintenance_mode: false,
        disable_youtube: false,
        disable_audius: false,
        disable_jamendo: false
      })
      .neq('id', 0)
      .select()
      .single();
    if (error) throw error;
    ctx.reply(`🟢 ${b('Enable All Executed!')}\n\n• Maintenance: ${b('OFF')}\n• YouTube: ${b('ENABLED')}\n• Audius: ${b('ENABLED')}\n• Jamendo: ${b('ENABLED')}`, { parse_mode: 'HTML', ...MAIN_KEYBOARD });
  } catch (e) {
    ctx.reply(`❌ Database Error: ${e.message}`);
  }
});

bot.command('maintenance', async (ctx) => {
  if (!isAdmin(ctx)) return;
  const param = ctx.message.text.split(' ')[1]?.toLowerCase();
  if (param !== 'on' && param !== 'off') return ctx.reply('⚠️ Usage: /maintenance <on|off>', MAIN_KEYBOARD);
  const val = (param === 'on');
  try {
    const { error } = await supabase.from('remote_config').update({ maintenance_mode: val }).neq('id', 0);
    if (error) throw error;
    ctx.reply(`🛠️ Maintenance mode set to: ${val ? '🚨 ' + b('ON (Blocking Users)') : '🟢 ' + b('OFF')}`, { parse_mode: 'HTML' });
  } catch (e) {
    ctx.reply(`❌ Database Error: ${e.message}. Ensure supabase has 'remote_config' table setup.`);
  }
});

bot.command('toggle_youtube', async (ctx) => {
  if (!isAdmin(ctx)) return;
  const param = ctx.message.text.split(' ')[1]?.toLowerCase();
  if (param !== 'on' && param !== 'off') return ctx.reply('⚠️ Usage: /toggle_youtube <on|off>', MAIN_KEYBOARD);
  const disable = (param === 'off');
  try {
    const { error } = await supabase.from('remote_config').update({ disable_youtube: disable }).neq('id', 0);
    if (error) throw error;
    ctx.reply(`📺 YouTube Stream set to: ${disable ? '❌ ' + b('DISABLED') : '🟢 ' + b('ENABLED')}`, { parse_mode: 'HTML' });
  } catch (e) {
    ctx.reply(`❌ Error: ${e.message}`);
  }
});

bot.command('toggle_audius', async (ctx) => {
  if (!isAdmin(ctx)) return;
  const param = ctx.message.text.split(' ')[1]?.toLowerCase();
  if (param !== 'on' && param !== 'off') return ctx.reply('⚠️ Usage: /toggle_audius <on|off>', MAIN_KEYBOARD);
  const disable = (param === 'off');
  try {
    const { error } = await supabase.from('remote_config').update({ disable_audius: disable }).neq('id', 0);
    if (error) throw error;
    ctx.reply(`🎧 Audius Stream set to: ${disable ? '❌ ' + b('DISABLED') : '🟢 ' + b('ENABLED')}`, { parse_mode: 'HTML' });
  } catch (e) {
    ctx.reply(`❌ Error: ${e.message}`);
  }
});

bot.command('toggle_jamendo', async (ctx) => {
  if (!isAdmin(ctx)) return;
  const param = ctx.message.text.split(' ')[1]?.toLowerCase();
  if (param !== 'on' && param !== 'off') return ctx.reply('⚠️ Usage: /toggle_jamendo <on|off>', MAIN_KEYBOARD);
  const disable = (param === 'off');
  try {
    const { error } = await supabase.from('remote_config').update({ disable_jamendo: disable }).neq('id', 0);
    if (error) throw error;
    ctx.reply(`🎵 Jamendo Stream set to: ${disable ? '❌ ' + b('DISABLED') : '🟢 ' + b('ENABLED')}`, { parse_mode: 'HTML' });
  } catch (e) {
    ctx.reply(`❌ Error: ${e.message}`);
  }
});

bot.command('set_playlists', async (ctx) => {
  if (!isAdmin(ctx)) return;
  const val = parseInt(ctx.message.text.split(' ')[1]);
  if (isNaN(val)) return ctx.reply('⚠️ Usage: /set_playlists <number>', MAIN_KEYBOARD);
  try {
    const { error } = await supabase.from('remote_config').update({ free_max_playlists: val }).neq('id', 0);
    if (error) throw error;
    ctx.reply(`📋 Max Free Playlists count updated to: ${b(val)}`, { parse_mode: 'HTML' });
  } catch (e) {
    ctx.reply(`❌ Error: ${e.message}`);
  }
});

bot.command('broadcast', async (ctx) => {
  if (!isAdmin(ctx)) return;
  const msg = ctx.message.text.split(' ').slice(1).join(' ').trim();
  if (!msg) return ctx.reply('⚠️ Usage: /broadcast <announcement message>', MAIN_KEYBOARD);
  try {
    const { error } = await supabase.from('broadcasts').insert({
      message: msg,
      sender: 'Admin',
      active: true,
      created_at: new Date().toISOString()
    });
    if (error) throw error;
    ctx.reply(`📢 ${b('Broadcast Broadcasted!')}\n\n${i(msg)}`, { parse_mode: 'HTML' });
  } catch (e) {
    ctx.reply(`❌ Error: ${e.message}. Ensure database has 'broadcasts' table setup.`);
  }
});

bot.command('verify', async (ctx) => {
  if (!isAdmin(ctx)) return;
  const email = ctx.message.text.split(' ').slice(1).join(' ').trim();
  if (!email) return ctx.reply('⚠️ Usage: /verify user@email.com', MAIN_KEYBOARD);
  try {
    const { data: user, error: e1 } = await supabase.from('profiles')
      .select('id, email, plan')
      .eq('email', email)
      .single();
    if (e1 || !user) return ctx.reply('🔍 User not found in database.', MAIN_KEYBOARD);
    const { error: e2 } = await supabase.from('profiles').update({ plan: 'PREMIUM_INDIVIDUAL' }).eq('id', user.id);
    if (e2) throw e2;
    ctx.reply(`✅ ${b('Verified')}\n${c(user.email)} → 🟡 ${b('PREMIUM_INDIVIDUAL')}`, { parse_mode: 'HTML', ...MAIN_KEYBOARD });
  } catch (e) {
    ctx.reply(`❌ Error: ${e.message}`, MAIN_KEYBOARD);
  }
});

bot.command('revoke', async (ctx) => {
  if (!isAdmin(ctx)) return;
  const email = ctx.message.text.split(' ').slice(1).join(' ').trim();
  if (!email) return ctx.reply('⚠️ Usage: /revoke user@email.com', MAIN_KEYBOARD);
  try {
    const { data: user, error: e1 } = await supabase.from('profiles')
      .select('id, email, plan')
      .eq('email', email)
      .single();
    if (e1 || !user) return ctx.reply('🔍 User not found.', MAIN_KEYBOARD);
    const { error: e2 } = await supabase.from('profiles').update({ plan: 'FREE' }).eq('id', user.id);
    if (e2) throw e2;
    ctx.reply(`❌ ${b('Revoked')}\n${c(user.email)} → ⚪ ${b('FREE')}`, { parse_mode: 'HTML', ...MAIN_KEYBOARD });
  } catch (e) {
    ctx.reply(`❌ Error: ${e.message}`, MAIN_KEYBOARD);
  }
});

bot.command('ban', async (ctx) => {
  if (!isAdmin(ctx)) return;
  const email = ctx.message.text.split(' ').slice(1).join(' ').trim();
  if (!email) return ctx.reply('⚠️ Usage: /ban user@email.com', MAIN_KEYBOARD);
  try {
    const { data: user, error: e1 } = await supabase.from('profiles').select('id, email').eq('email', email).single();
    if (e1 || !user) return ctx.reply('🔍 User not found.', MAIN_KEYBOARD);
    const { error: e2 } = await supabase.from('profiles').update({ is_banned: true }).eq('id', user.id);
    if (e2) throw e2;
    ctx.reply(`🚨 ${b('User Banned')}\n${c(user.email)} has been suspended.`, { parse_mode: 'HTML', ...MAIN_KEYBOARD });
  } catch (e) {
    ctx.reply(`❌ Error: ${e.message}`);
  }
});

bot.command('unban', async (ctx) => {
  if (!isAdmin(ctx)) return;
  const email = ctx.message.text.split(' ').slice(1).join(' ').trim();
  if (!email) return ctx.reply('⚠️ Usage: /unban user@email.com', MAIN_KEYBOARD);
  try {
    const { data: user, error: e1 } = await supabase.from('profiles').select('id, email').eq('email', email).single();
    if (e1 || !user) return ctx.reply('🔍 User not found.', MAIN_KEYBOARD);
    const { error: e2 } = await supabase.from('profiles').update({ is_banned: false }).eq('id', user.id);
    if (e2) throw e2;
    ctx.reply(`🟢 ${b('User Unbanned')}\n${c(user.email)} suspension lifted.`, { parse_mode: 'HTML', ...MAIN_KEYBOARD });
  } catch (e) {
    ctx.reply(`❌ Error: ${e.message}`);
  }
});

bot.command('retention', async (ctx) => {
  if (!isAdmin(ctx)) return;
  try {
    const d7 = new Date(Date.now() - 7 * 864e5).toISOString();
    const d30 = new Date(Date.now() - 30 * 864e5).toISOString();
    const { data: week } = await supabase.from('listening_history').select('user_id').gte('played_at', d7);
    const { data: month } = await supabase.from('listening_history').select('user_id').gte('played_at', d30);
    const active7 = week ? new Set(week.map(r => r.user_id)).size : 0;
    const active30 = month ? new Set(month.map(r => r.user_id)).size : 0;
    const { count: total } = await supabase.from('profiles').select('*', { count: 'exact', head: true });
    
    ctx.reply(
      `📈 ${b('App Retention Rates')}\n` +
      hr() +
      `👥 Total User Base: ${b(total)}\n` +
      `• Weekly Active (WAU): ${b(active7)} (${((active7/total)*100).toFixed(1)}%)\n` +
      `• Monthly Active (MAU): ${b(active30)} (${((active30/total)*100).toFixed(1)}%)`,
      { parse_mode: 'HTML', ...MAIN_KEYBOARD }
    );
  } catch (e) {
    ctx.reply(`❌ Error: ${e.message}`, MAIN_KEYBOARD);
  }
});

// ========== INLINE CALLBACKS AND PENDING REQUESTS ==========

bot.action(/approve_(.+)/, async (ctx) => {
  if (!isAdmin(ctx)) return;
  const id = ctx.match[1];
  try {
    const { data: req, error: e1 } = await supabase.from('premium_requests').select('*').eq('id', id).single();
    if (e1 || !req) return ctx.answerCbQuery('🔍 Request not found.');
    const { data: profile } = await supabase.from('profiles').select('id').eq('email', req.email).single();
    if (profile) {
      await supabase.from('profiles').update({ plan: req.plan }).eq('id', profile.id);
    }
    await supabase.from('premium_requests').update({ status: 'approved', updated_at: new Date().toISOString() }).eq('id', id);
    await ctx.editMessageText(
      `✅ ${b('Approved')}\n${c(req.email)} → 🟡 ${b(req.plan)}`,
      { parse_mode: 'HTML' }
    );
    await ctx.answerCbQuery('✅ Approved!');
  } catch (e) {
    ctx.answerCbQuery(`❌ Error: ${e.message}`);
  }
});

bot.action(/reject_(.+)/, async (ctx) => {
  if (!isAdmin(ctx)) return;
  const id = ctx.match[1];
  try {
    const { data: req, error: e1 } = await supabase.from('premium_requests').select('*').eq('id', id).single();
    if (e1 || !req) return ctx.answerCbQuery('🔍 Request not found.');
    await supabase.from('premium_requests').update({ status: 'rejected', updated_at: new Date().toISOString() }).eq('id', id);
    await ctx.editMessageText(
      `❌ ${b('Rejected')}\n${c(req.email)}`,
      { parse_mode: 'HTML' }
    );
    await ctx.answerCbQuery('❌ Rejected.');
  } catch (e) {
    ctx.answerCbQuery(`❌ Error: ${e.message}`);
  }
});

// Callback action: Disable All
bot.action('cfg_disable_all', async (ctx) => {
  if (!isAdmin(ctx)) return ctx.answerCbQuery('🔒 Unauthorized');
  try {
    const { data, error } = await supabase
      .from('remote_config')
      .update({
        maintenance_mode: true,
        disable_youtube: true,
        disable_audius: true,
        disable_jamendo: true
      })
      .neq('id', 0)
      .select()
      .single();
    if (error) throw error;
    await ctx.answerCbQuery('🚨 All services disabled / Maintenance ON!');
    await sendOrEditConfig(ctx, data, true);
  } catch (e) {
    await ctx.answerCbQuery(`❌ Error: ${e.message}`);
  }
});

// Callback action: Enable All
bot.action('cfg_enable_all', async (ctx) => {
  if (!isAdmin(ctx)) return ctx.answerCbQuery('🔒 Unauthorized');
  try {
    const { data, error } = await supabase
      .from('remote_config')
      .update({
        maintenance_mode: false,
        disable_youtube: false,
        disable_audius: false,
        disable_jamendo: false
      })
      .neq('id', 0)
      .select()
      .single();
    if (error) throw error;
    await ctx.answerCbQuery('🟢 All services enabled / Maintenance OFF!');
    await sendOrEditConfig(ctx, data, true);
  } catch (e) {
    await ctx.answerCbQuery(`❌ Error: ${e.message}`);
  }
});

// Callback action: Toggle Maintenance
bot.action('toggle_maint', async (ctx) => {
  if (!isAdmin(ctx)) return ctx.answerCbQuery('🔒 Unauthorized');
  try {
    let { data: current } = await supabase.from('remote_config').select('*').limit(1).single();
    const target = !current.maintenance_mode;
    const { data, error } = await supabase
      .from('remote_config')
      .update({ maintenance_mode: target })
      .neq('id', 0)
      .select()
      .single();
    if (error) throw error;
    await ctx.answerCbQuery(`Maintenance Mode: ${target ? 'ON' : 'OFF'}`);
    await sendOrEditConfig(ctx, data, true);
  } catch (e) {
    await ctx.answerCbQuery(`❌ Error: ${e.message}`);
  }
});

// Callback action: Toggle YouTube
bot.action('toggle_yt', async (ctx) => {
  if (!isAdmin(ctx)) return ctx.answerCbQuery('🔒 Unauthorized');
  try {
    let { data: current } = await supabase.from('remote_config').select('*').limit(1).single();
    const target = !current.disable_youtube;
    const { data, error } = await supabase
      .from('remote_config')
      .update({ disable_youtube: target })
      .neq('id', 0)
      .select()
      .single();
    if (error) throw error;
    await ctx.answerCbQuery(`YouTube: ${target ? 'DISABLED' : 'ENABLED'}`);
    await sendOrEditConfig(ctx, data, true);
  } catch (e) {
    await ctx.answerCbQuery(`❌ Error: ${e.message}`);
  }
});

// Callback action: Toggle Audius
bot.action('toggle_aud', async (ctx) => {
  if (!isAdmin(ctx)) return ctx.answerCbQuery('🔒 Unauthorized');
  try {
    let { data: current } = await supabase.from('remote_config').select('*').limit(1).single();
    const target = !current.disable_audius;
    const { data, error } = await supabase
      .from('remote_config')
      .update({ disable_audius: target })
      .neq('id', 0)
      .select()
      .single();
    if (error) throw error;
    await ctx.answerCbQuery(`Audius: ${target ? 'DISABLED' : 'ENABLED'}`);
    await sendOrEditConfig(ctx, data, true);
  } catch (e) {
    await ctx.answerCbQuery(`❌ Error: ${e.message}`);
  }
});

// Callback action: Toggle Jamendo
bot.action('toggle_jam', async (ctx) => {
  if (!isAdmin(ctx)) return ctx.answerCbQuery('🔒 Unauthorized');
  try {
    let { data: current } = await supabase.from('remote_config').select('*').limit(1).single();
    const target = !current.disable_jamendo;
    const { data, error } = await supabase
      .from('remote_config')
      .update({ disable_jamendo: target })
      .neq('id', 0)
      .select()
      .single();
    if (error) throw error;
    await ctx.answerCbQuery(`Jamendo: ${target ? 'DISABLED' : 'ENABLED'}`);
    await sendOrEditConfig(ctx, data, true);
  } catch (e) {
    await ctx.answerCbQuery(`❌ Error: ${e.message}`);
  }
});

bot.launch();
console.log('🍉 Watermelon Admin Bot version 2.0 fully functional');

process.once('SIGINT', () => bot.stop('SIGINT'));
process.once('SIGTERM', () => bot.stop('SIGTERM'));
