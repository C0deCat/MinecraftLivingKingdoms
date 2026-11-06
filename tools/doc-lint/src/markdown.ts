// Minimal markdown parsing: links, headings and frontmatter. No dependencies on purpose.

export interface Link {
  target: string;
  line: number;
}

/** Returns lines with fenced code blocks and inline code blanked out, keeping line numbers stable. */
export function stripCode(text: string): string[] {
  const lines = text.split(/\r?\n/);
  let inFence = false;
  return lines.map((line) => {
    if (/^\s*(```|~~~)/.test(line)) {
      inFence = !inFence;
      return "";
    }
    if (inFence) return "";
    return line.replace(/`[^`]*`/g, "");
  });
}

/** Inline markdown links `[text](target)`; images included. Code is ignored. */
export function extractLinks(text: string): Link[] {
  const links: Link[] = [];
  const pattern = /\[(?:[^\]\[]|\[[^\]]*\])*\]\(([^)\s]+)(?:\s+"[^"]*")?\)/g;
  stripCode(text).forEach((line, index) => {
    for (const match of line.matchAll(pattern)) {
      links.push({ target: match[1], line: index + 1 });
    }
  });
  return links;
}

/** GitHub-style heading anchors, including `-1`, `-2` suffixes for duplicates. */
export function headingAnchors(text: string): Set<string> {
  const anchors = new Set<string>();
  const seen = new Map<string, number>();
  for (const line of stripCode(text)) {
    const match = /^#{1,6}\s+(.*?)\s*#*\s*$/.exec(line);
    if (!match) continue;
    const base = slugify(match[1]);
    const count = seen.get(base) ?? 0;
    seen.set(base, count + 1);
    anchors.add(count === 0 ? base : `${base}-${count}`);
  }
  return anchors;
}

export function slugify(heading: string): string {
  return heading
    .replace(/\[([^\]]*)\]\([^)]*\)/g, "$1") // keep link text only
    .replace(/[*_]/g, "")
    .trim()
    .toLowerCase()
    .replace(/[^\p{L}\p{N}\s-]/gu, "")
    .replace(/\s/g, "-");
}

export interface Frontmatter {
  fields: Map<string, string>;
  present: boolean;
}

export function parseFrontmatter(text: string): Frontmatter {
  const fields = new Map<string, string>();
  const match = /^---\r?\n([\s\S]*?)\r?\n---(\r?\n|$)/.exec(text);
  if (!match) return { fields, present: false };
  for (const line of match[1].split(/\r?\n/)) {
    const kv = /^([A-Za-z_][\w-]*):\s*(.*)$/.exec(line);
    if (kv) fields.set(kv[1], kv[2].trim());
  }
  return { fields, present: true };
}

export function headings(text: string): string[] {
  return stripCode(text)
    .map((line) => /^#{1,6}\s+(.*?)\s*$/.exec(line)?.[1])
    .filter((h): h is string => h !== undefined);
}
