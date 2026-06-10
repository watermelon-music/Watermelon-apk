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

const MAIN_KEYBOARD = Markup.keyboard([
  ['🍉 Dashboard', '👥 Users'],
  ['📊 Stats', '📅 Daily'],
  ['⏳ Pending', '🏆 Top Users'],
  ['🆕 Recent', '📈 Retention'],
  ['🎵 Plays', '💎 Subs']
]).resize();

function b(text) { return `<b>${text}</b>`; }
function i(text) { return `<i>${text}</i>`; }
function c(text) { return `<code>${text}</code>`; }

// ========== START ==========

bot.start((ctx) => {
  if (!isAdmin(ctx)) {
    return ctx.reply(
      '🍉 <b>Watermelon Music</b>\n\n' +
      'This is the admin bot for Watermelon app.\n' +
      'If you are the admin, set your chat ID in the environment.\n\n' +
      'Your chat ID: <code>' + (ctx.chat?.id || ctx.from?.id) + '</code>\n' +
      'Use /myid to copy it anytime.',
      { parse_mode: 'HTML' }
    );
  }
  ctx.reply(
    '🍉 <b>Watermelon Admin Bot</b>\n\n' +
    '👋 Welcome back, chief!\n' +
    'Use the keyboard below or type /commands for the full list.',
    { parse_mode: 'HTML', ...MAIN_KEYBOARD }
  );
});

bot.command('myid', (ctx) => {
  const id = ctx.chat?.id || ctx.from?.id;
  ctx.reply(
    '🆔 <b>Your Chat ID</b>\n\n<code>' + id + '</code>\n\n' +
    'Set this as <code>TELEGRAM_ADMIN_CHAT_ID</code> on Render to become admin.',
    { parse_mode: 'HTML' }
  );
});

bot.command('commands', (ctx) => {
  if (!isAdmin(ctx)) return;
  ctx.reply(
    '🍉 <b>Watermelon Commands</b>\n\n' +
    '👥 <b>/users</b> — Total, free & paid users\n' +
    '💎 <b>/subs</b> — Recent premium subscribers\n' +
    '🎵 <b>/plays</b> — Total plays & top songs\n' +
    '📅 <b>/daily</b> — Plays & signups (24h)\n' +
    '🏆 <b>/topusers</b> — Most active users\n' +
    '🆕 <b>/recent</b> — New signups today\n' +
    '📈 <b>/retention</b> — Active users (7d / 30d)\n' +
    '⏳ <b>/pending</b> — Pending premium requests\n' +
    '📊 <b>/stats</b> — Combined dashboard\n' +
    '✅ <b>/verify</b> &lt;email&gt; — Approve premium\n' +
    '❌ <b>/revoke</b> &lt;email&gt; — Revoke premium',
    { parse_mode: 'HTML', ...MAIN_KEYBOARD }
  );
});

// ========== KEYBOARD HANDLERS ==========

bot.hears('🍉 Dashboard', (ctx) => ctx.reply('/stats'));
bot.hears('👥 Users', (ctx) => ctx.reply('/users'));
bot.hears('📊 Stats', (ctx) => ctx.reply('/stats'));
bot.hears('📅 Daily', (ctx) => ctx.reply('/daily'));
bot.hears('⏳ Pending', (ctx) => ctx.reply('/pending'));
bot.hears('🏆 Top Users', (ctx) => ctx.reply('/topusers'));
bot.hears('🆕 Recent', (ctx) => ctx.reply('/recent'));
bot.hears('📈 Retention', (ctx) => ctx.reply('/retention'));
bot.hears('🎵 Plays', (ctx) => ctx.reply('/plays'));
bot.hears('💎 Subs', (ctx) => ctx.reply('/subs'));

// ========== COMMANDS ==========

bot.command('users', async (ctx) => {
  if (!isAdmin(ctx)) return;
  try {
    const { count: total, error: e1 } = await supabase.from('profiles').select('*', { count: 'exact', head: true });
    const { count: free, error: e2 } = await supabase.from('profiles').select('*', { count: 'exact', head: true }).eq('plan', 'FREE');
    const { count: paid, error: e3 } = await supabase.from('profiles').select('*', { count: 'exact', head: true }).neq('plan', 'FREE');
    if (e1 || e2 || e3) throw e1 || e2 || e3;
    ctx.reply(
      `👥 ${b('Users')}\n\n` +
      `🟢 Total: ${b(total)}\n` +
      `⚪ Free: ${b(free)}\n` +
      `🟡 Paid: ${b(paid)}`,
      { parse_mode: 'HTML', ...MAIN_KEYBOARD }
    );
  } catch (e) {
    ctx.reply(`❌ Error: ${e.message}`, MAIN_KEYBOARD);
  }
});

bot.command('subs', async (ctx) => {
  if (!isAdmin(ctx)) return;
  try {
    const { data, error } = await supabase.from('profiles')
      .select('email, plan, created_at')
      .neq('plan', 'FREE')
      .order('created_at', { ascending: false })
      .limit(20);
    if (error) throw error;
    const lines = data.map(u => `💎 ${c(u.email)} | ${b(u.plan)} | ${u.created_at?.slice(0,10)||''}`).join('\n');
    ctx.reply(`💎 ${b('Recent Premium Users')}\n\n${lines || '📭 None'}`, { parse_mode: 'HTML', ...MAIN_KEYBOARD });
  } catch (e) {
    ctx.reply(`❌ Error: ${e.message}`, MAIN_KEYBOARD);
  }
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
      top = '\n\n🎵 ' + b('Top Songs:') + '\n' + topSongs.map((s, i) => `${i+1}. ${s.title} (${s.plays} plays)`).join('\n');
    }
    ctx.reply(
      `🎵 ${b('Plays')}\n` +
      `🎧 Total: ${b(totalPlays)}\n` +
      `📅 Last 24h: ${b(todayPlays)}${top}`,
      { parse_mode: 'HTML', ...MAIN_KEYBOARD }
    );
  } catch (e) {
    ctx.reply(`❌ Error: ${e.message}`, MAIN_KEYBOARD);
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
    if (e1 || !user) return ctx.reply('🔍 User not found.', MAIN_KEYBOARD);
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

bot.command('stats', async (ctx) => {
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
      `🍉 ${b('Dashboard')}\n\n` +
      `👥 Total: ${b(users)}\n` +
      `   ⚪ Free: ${free}\n` +
      `   🟡 Paid: ${paid}\n\n` +
      `📋 Playlists: ${b(playlists)}\n` +
      `⭐ Favorites: ${b(favorites)}\n\n` +
      `🎵 Total plays: ${b(plays)}\n` +
      `📅 Plays (24h): ${b(todayPlays)}`,
      { parse_mode: 'HTML', ...MAIN_KEYBOARD }
    );
  } catch (e) {
    ctx.reply(`❌ Error: ${e.message}`, MAIN_KEYBOARD);
  }
});

bot.command('daily', async (ctx) => {
  if (!isAdmin(ctx)) return;
  try {
    const yesterday = new Date(Date.now() - 864e5).toISOString();
    const { count: todayPlays } = await supabase.from('listening_history').select('*', { count: 'exact', head: true }).gte('played_at', yesterday);
    const { count: todaySignups } = await supabase.from('profiles').select('*', { count: 'exact', head: true }).gte('created_at', yesterday);
    const { data: topSong } = await supabase.from('listening_history').select('title, artist').gte('played_at', yesterday).limit(1).order('played_at', { ascending: false });
    const top = topSong?.[0] ? `${topSong[0].title} — ${topSong[0].artist || 'Unknown'}` : '📭 No plays yet';
    ctx.reply(
      `📅 ${b('Daily Digest')} ${i('(last 24h)')}\n\n` +
      `🎵 Plays: ${b(todayPlays)}\n` +
      `🆕 Signups: ${b(todaySignups)}\n` +
      `🔥 Top song: ${i(top)}`,
      { parse_mode: 'HTML', ...MAIN_KEYBOARD }
    );
  } catch (e) {
    ctx.reply(`❌ Error: ${e.message}`, MAIN_KEYBOARD);
  }
});

bot.command('topusers', async (ctx) => {
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
    ctx.reply(`🏆 ${b('Top Users')}\n\n${lines}`, { parse_mode: 'HTML', ...MAIN_KEYBOARD });
  } catch (e) {
    ctx.reply(`❌ Error: ${e.message}`, MAIN_KEYBOARD);
  }
});

bot.command('recent', async (ctx) => {
  if (!isAdmin(ctx)) return;
  try {
    const yesterday = new Date(Date.now() - 864e5).toISOString();
    const { data, error } = await supabase.from('profiles').select('email, display_name, created_at').gte('created_at', yesterday).order('created_at', { ascending: false }).limit(20);
    if (error) throw error;
    if (!data.length) return ctx.reply('📭 No new signups today.', MAIN_KEYBOARD);
    const lines = data.map(u => `🆕 ${b(u.display_name || u.email)} — ${u.created_at?.slice(0, 10)}`).join('\n');
    ctx.reply(`🆕 ${b('Recent Signups')} ${i('(24h)')}\n\n${lines}`, { parse_mode: 'HTML', ...MAIN_KEYBOARD });
  } catch (e) {
    ctx.reply(`❌ Error: ${e.message}`, MAIN_KEYBOARD);
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
      `📈 ${b('Retention')}\n\n` +
      `👥 Total: ${b(total)}\n` +
      `🕐 Active (7d): ${b(active7)}\n` +
      `🕐 Active (30d): ${b(active30)}`,
      { parse_mode: 'HTML', ...MAIN_KEYBOARD }
    );
  } catch (e) {
    ctx.reply(`❌ Error: ${e.message}`, MAIN_KEYBOARD);
  }
});

bot.command('pending', async (ctx) => {
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
});

// ========== INLINE CALLBACKS ==========

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

bot.launch();
console.log('🍉 Watermelon Telegram admin bot started');

process.once('SIGINT', () => bot.stop('SIGINT'));
process.once('SIGTERM', () => bot.stop('SIGTERM'));
