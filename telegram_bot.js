const { Telegraf } = require('telegraf');
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

// ========== COMMANDS ==========

bot.start((ctx) => {
  if (!isAdmin(ctx)) return ctx.reply('Unauthorized.');
  ctx.reply('Watermelon Admin Bot\n\nCommands:\n/users\n/subs\n/plays\n/verify <email>\n/revoke <email>');
});

bot.command('users', async (ctx) => {
  if (!isAdmin(ctx)) return;
  try {
    const { count: total, error: e1 } = await supabase.from('profiles').select('*', { count: 'exact', head: true });
    const { count: free, error: e2 } = await supabase.from('profiles').select('*', { count: 'exact', head: true }).eq('plan', 'FREE');
    const { count: paid, error: e3 } = await supabase.from('profiles').select('*', { count: 'exact', head: true }).neq('plan', 'FREE');
    if (e1 || e2 || e3) throw e1 || e2 || e3;
    ctx.reply(`👥 Users\nTotal: ${total}\nFree: ${free}\nPaid: ${paid}`);
  } catch (e) {
    ctx.reply(`Error: ${e.message}`);
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
    const lines = data.map(u => `• ${u.email} | ${u.plan} | ${u.created_at?.slice(0,10)||''}`).join('\n');
    ctx.reply(`💎 Recent Premium Users\n\n${lines || 'None'}`);
  } catch (e) {
    ctx.reply(`Error: ${e.message}`);
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
      top = '\n\nTop Songs:\n' + topSongs.map((s, i) => `${i+1}. ${s.title} (${s.plays})`).join('\n');
    }
    ctx.reply(`🎵 Plays\nTotal: ${totalPlays}\nLast 24h: ${todayPlays}${top}`);
  } catch (e) {
    ctx.reply(`Error: ${e.message}`);
  }
});

bot.command('verify', async (ctx) => {
  if (!isAdmin(ctx)) return;
  const email = ctx.message.text.split(' ').slice(1).join(' ').trim();
  if (!email) return ctx.reply('Usage: /verify user@email.com');
  try {
    const { data: user, error: e1 } = await supabase.from('profiles')
      .select('id, email, plan')
      .eq('email', email)
      .single();
    if (e1 || !user) return ctx.reply('User not found.');
    const { error: e2 } = await supabase.from('profiles').update({ plan: 'PREMIUM_INDIVIDUAL' }).eq('id', user.id);
    if (e2) throw e2;
    ctx.reply(`✅ Verified\n${user.email} → PREMIUM_INDIVIDUAL`);
  } catch (e) {
    ctx.reply(`Error: ${e.message}`);
  }
});

bot.command('revoke', async (ctx) => {
  if (!isAdmin(ctx)) return;
  const email = ctx.message.text.split(' ').slice(1).join(' ').trim();
  if (!email) return ctx.reply('Usage: /revoke user@email.com');
  try {
    const { data: user, error: e1 } = await supabase.from('profiles')
      .select('id, email, plan')
      .eq('email', email)
      .single();
    if (e1 || !user) return ctx.reply('User not found.');
    const { error: e2 } = await supabase.from('profiles').update({ plan: 'FREE' }).eq('id', user.id);
    if (e2) throw e2;
    ctx.reply(`⛔ Revoked\n${user.email} → FREE`);
  } catch (e) {
    ctx.reply(`Error: ${e.message}`);
  }
});

bot.command('stats', async (ctx) => {
  if (!isAdmin(ctx)) return;
  try {
    const { count: users } = await supabase.from('profiles').select('*', { count: 'exact', head: true });
    const { count: playlists } = await supabase.from('playlists').select('*', { count: 'exact', head: true });
    const { count: favorites } = await supabase.from('favorites').select('*', { count: 'exact', head: true });
    const { count: plays } = await supabase.from('listening_history').select('*', { count: 'exact', head: true });
    ctx.reply(`📊 Stats\nUsers: ${users}\nPlaylists: ${playlists}\nFavorites: ${favorites}\nPlays: ${plays}`);
  } catch (e) {
    ctx.reply(`Error: ${e.message}`);
  }
});

bot.launch();
console.log('Watermelon Telegram admin bot started');

// Graceful stop
process.once('SIGINT', () => bot.stop('SIGINT'));
process.once('SIGTERM', () => bot.stop('SIGTERM'));
