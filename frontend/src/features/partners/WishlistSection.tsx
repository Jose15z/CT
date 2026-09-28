import { useState } from 'react'
import type { FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { Check, ExternalLink, Plus, Trash2 } from 'lucide-react'
import { Button } from '../../components/ui/Button'
import { Input } from '../../components/ui/Input'
import { ConfirmDialog } from '../../components/ui/ConfirmDialog'
import { useToast } from '../../components/ui/Toast'
import { useCreateWish, useDeleteWish, useUpdateWish, useWishlist } from '../../lib/queries'
import { errorMessage } from '../../lib/errors'

/** Owner-only gift ideas for a partner; the advice engine reads the open count. */
export function WishlistSection({ partnerId }: { partnerId: string }) {
  const { t } = useTranslation()
  const toast = useToast()
  const { data: wishes } = useWishlist(partnerId)
  const createWish = useCreateWish(partnerId)
  const updateWish = useUpdateWish(partnerId)
  const deleteWish = useDeleteWish(partnerId)
  const [title, setTitle] = useState('')
  const [deleteId, setDeleteId] = useState<string | null>(null)

  const submit = (e: FormEvent) => {
    e.preventDefault()
    if (!title.trim()) return
    createWish.mutate(
      { title: title.trim() },
      {
        onSuccess: () => setTitle(''),
        onError: (err) => toast(errorMessage(err), 'error'),
      },
    )
  }

  return (
    <section>
      <h2 className="text-[15px] font-semibold tracking-tight text-ink">{t('wishlist.title')}</h2>
      <p className="mt-0.5 text-[12px] text-ink-3">{t('wishlist.hint')}</p>

      <form onSubmit={submit} className="mt-3 flex gap-2">
        <Input
          aria-label={t('wishlist.title')}
          placeholder={t('wishlist.placeholder')}
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          maxLength={120}
        />
        <Button
          type="submit"
          variant="secondary"
          icon={<Plus size={14} />}
          disabled={createWish.isPending || !title.trim()}
        >
          {t('wishlist.add')}
        </Button>
      </form>

      {wishes && wishes.length > 0 ? (
        <ul className="mt-3 divide-y divide-border">
          {wishes.map((wish) => (
            <li key={wish.id} className="flex items-center gap-3 py-2 text-[13.5px]">
              <button
                role="checkbox"
                aria-checked={wish.done}
                aria-label={wish.done ? t('wishlist.markPending') : t('wishlist.markDone')}
                title={wish.done ? t('wishlist.markPending') : t('wishlist.markDone')}
                disabled={updateWish.isPending}
                onClick={() => updateWish.mutate({ id: wish.id, done: !wish.done })}
                className={`flex h-4.5 w-4.5 shrink-0 items-center justify-center rounded-[5px] border transition-colors ${
                  wish.done
                    ? 'border-teal bg-teal text-white'
                    : 'border-border-strong bg-surface hover:border-teal'
                }`}
              >
                {wish.done && <Check size={11} strokeWidth={3} aria-hidden="true" />}
              </button>
              <span className={`min-w-0 flex-1 truncate ${wish.done ? 'text-ink-3 line-through' : 'text-ink'}`}>
                {wish.title}
              </span>
              {wish.url && (
                <a
                  href={wish.url}
                  target="_blank"
                  rel="noreferrer"
                  className="rounded-md p-1 text-ink-3 hover:bg-surface-2 hover:text-ink"
                  aria-label={wish.url}
                >
                  <ExternalLink size={13} />
                </a>
              )}
              <button
                onClick={() => setDeleteId(wish.id)}
                aria-label={t('common.delete')}
                className="rounded-md p-1 text-ink-3 transition-colors hover:bg-surface-2 hover:text-danger"
              >
                <Trash2 size={13} />
              </button>
            </li>
          ))}
        </ul>
      ) : (
        <p className="mt-3 text-[13px] text-ink-3">{t('wishlist.empty')}</p>
      )}

      <ConfirmDialog
        open={deleteId !== null}
        title={t('wishlist.deleteConfirm')}
        body={t('common.irreversible')}
        danger
        busy={deleteWish.isPending}
        onConfirm={() => deleteId && deleteWish.mutate(deleteId, { onSuccess: () => setDeleteId(null) })}
        onClose={() => setDeleteId(null)}
      />
    </section>
  )
}
