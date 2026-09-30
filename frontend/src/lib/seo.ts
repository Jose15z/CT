import { useEffect } from 'react'
import { useLocation } from 'react-router-dom'

export const SITE_URL = 'https://culitostracker.vercel.app'

type PageMeta = {
  /** Full document title; the brand suffix is the caller's choice. */
  title: string
  /** Omitted: the description baked into index.html stays. */
  description?: string
  /** Only public marketing/legal pages are indexable; everything else is noindex. */
  index?: boolean
}

function setMeta(selector: string, attr: 'name' | 'property', key: string, content: string) {
  let tag = document.head.querySelector<HTMLMetaElement>(selector)
  if (!tag) {
    tag = document.createElement('meta')
    tag.setAttribute(attr, key)
    document.head.appendChild(tag)
  }
  tag.content = content
}

function setCanonical(href: string) {
  let link = document.head.querySelector<HTMLLinkElement>('link[rel="canonical"]')
  if (!link) {
    link = document.createElement('link')
    link.rel = 'canonical'
    document.head.appendChild(link)
  }
  link.href = href
}

const baseDescription =
  typeof document === 'undefined'
    ? ''
    : (document.head.querySelector<HTMLMetaElement>('meta[name="description"]')?.content ?? '')

/**
 * Per-route <head> for a client-rendered app: title, description, robots and
 * canonical follow the page, so search engines and link previews see the
 * page they landed on rather than the shell's defaults.
 */
export function usePageMeta({ title, description, index = false }: PageMeta) {
  const { pathname } = useLocation()

  useEffect(() => {
    const canonical = SITE_URL + (pathname === '/' ? '/' : pathname.replace(/\/+$/, ''))
    const text = description ?? baseDescription
    document.title = title
    setMeta('meta[name="description"]', 'name', 'description', text)
    setMeta('meta[name="robots"]', 'name', 'robots', index ? 'index, follow' : 'noindex, nofollow')
    setMeta('meta[property="og:title"]', 'property', 'og:title', title)
    setMeta('meta[property="og:description"]', 'property', 'og:description', text)
    setMeta('meta[property="og:url"]', 'property', 'og:url', canonical)
    setMeta('meta[name="twitter:title"]', 'name', 'twitter:title', title)
    setMeta('meta[name="twitter:description"]', 'name', 'twitter:description', text)
    setCanonical(canonical)
  }, [title, description, index, pathname])
}
