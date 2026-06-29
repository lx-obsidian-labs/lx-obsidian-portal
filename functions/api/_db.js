export function db(context) {
  return context.env.DB;
}

export async function all(context, sql, ...params) {
  return await db(context).prepare(sql).bind(...params).all();
}

export async function get(context, sql, ...params) {
  return await db(context).prepare(sql).bind(...params).first();
}

export async function run(context, sql, ...params) {
  return await db(context).prepare(sql).bind(...params).run();
}
