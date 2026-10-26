document.addEventListener('DOMContentLoaded', () => {

  document.querySelectorAll('.mb-password').forEach((wrapper) => {
    const input = wrapper.querySelector('input');
    const button = wrapper.querySelector('button');
    if (!input || !button) return;
    button.addEventListener('click', () => {
      const show = input.type === 'password';
      input.type = show ? 'text' : 'password';
      const label = show ? 'Ocultar senha' : 'Mostrar senha';
      button.setAttribute('aria-label', label);
      button.setAttribute('title', label);
    });
  });

  document.querySelectorAll('dialog[data-aberto="true"]').forEach((dialog) => dialog.showModal());

  document.querySelectorAll('[data-dialog-open]').forEach((trigger) => {
    trigger.addEventListener('click', () => {
      const dialog = document.getElementById(trigger.dataset.dialogOpen);
      if (dialog) dialog.showModal();
    });
  });
  document.querySelectorAll('[data-dialog-close]').forEach((trigger) => {
    trigger.addEventListener('click', () => trigger.closest('dialog')?.close());
  });

  document.querySelectorAll('.mb-navbar__menu').forEach((button) => {
    button.addEventListener('click', () => {
      const navbar = button.closest('.mb-navbar');
      const open = navbar.classList.toggle('is-open');
      button.setAttribute('aria-expanded', String(open));
    });
  });
});

function pollOrderStatus(section) {
  const url = section.dataset.orderStatusUrl;
  const waitEmail = section.dataset.orderWait === 'email';
  let attempts = 0;
  const timer = setInterval(async () => {
    if (++attempts > 45) {
      clearInterval(timer);
      return;
    }
    try {
      const response = await fetch(url, { headers: { Accept: 'application/json' }, credentials: 'same-origin' });
      const type = response.headers.get('Content-Type') || '';
      if (!response.ok || !type.includes('application/json')) {
        clearInterval(timer);
        return;
      }
      const body = await response.json();
      const done = waitEmail ? body.statusEmail : body.status && body.status !== 'AWAITING_PAYMENT';
      if (done) {
        clearInterval(timer);
        window.location.reload();
      }
    } catch (e) {
      clearInterval(timer);
    }
  }, 2000);
}

document.addEventListener('DOMContentLoaded', () => {
  document.querySelectorAll('[data-order-status-url]').forEach(pollOrderStatus);
});

document.addEventListener('DOMContentLoaded', () => {
  const brl = (value) => 'R$ ' + value.toFixed(2).replace('.', ',');
  const round2 = (value) => Math.round((value + Number.EPSILON) * 100) / 100;
  const num = (input) => {
    const n = Number(String(input?.value ?? '').replace(',', '.'));
    return input && input.value !== '' && Number.isFinite(n) ? n : NaN;
  };

  document.querySelectorAll('[data-stepper]').forEach((stepper) => {
    const input = stepper.querySelector('input');
    const form = stepper.closest('form');
    const buy = form?.querySelector('[data-unit-price]');
    const subtotal = form?.querySelector('[data-subtotal]');
    const clamp = () => {
      const min = Number(input.min || 1);
      const max = Number(input.max || Infinity);
      const value = Math.trunc(Number(input.value) || min);
      input.value = String(Math.min(max, Math.max(min, value)));
      if (buy && subtotal) subtotal.textContent = brl(round2(Number(buy.dataset.unitPrice) * Number(input.value)));
    };
    stepper.querySelectorAll('[data-step]').forEach((button) => {
      button.addEventListener('click', () => {
        input.value = String(Number(input.value || 0) + Number(button.dataset.step));
        clamp();
      });
    });
    input.addEventListener('change', clamp);
  });

  const productModal = document.getElementById('product-modal');
  const productForm = productModal?.querySelector('[data-product-form]');
  if (productModal && productForm) {
    const field = (name) => productForm.elements.namedItem(name);
    const offerToggle = productForm.querySelector('[data-offer-toggle]');
    const offerFields = productForm.querySelector('[data-offer-fields]');
    const deleteButton = productForm.querySelector('[data-modal-delete]');

    const preview = () => {
      const cost = num(field('cost'));
      const markup = num(field('markupPercentage'));
      const discount = offerToggle.checked ? (num(field('discountPercentage')) || 0) : 0;
      const ok = cost > 0 && markup >= 0 && markup <= 500;
      const price = ok ? round2(cost * (1 + markup / 100)) : NaN;
      const offer = ok ? round2(price * (1 - Math.min(Math.max(discount, 0), 90) / 100)) : NaN;
      productForm.querySelector('[data-preview-price]').value = ok ? price.toFixed(2).replace('.', ',') : '—';
      productForm.querySelector('[data-preview-profit]').value = ok ? round2(price - cost).toFixed(2).replace('.', ',') : '—';
      productForm.querySelector('[data-preview-offer]').textContent = ok ? brl(offer) : '—';
      productForm.querySelector('[data-preview-old]').textContent = ok ? brl(price) : '';
    };
    const syncOffer = () => {
      offerFields.hidden = !offerToggle.checked;
    };
    offerToggle.addEventListener('change', () => {
      const discount = field('discountPercentage');
      if (!offerToggle.checked) discount.value = '0';
      else if (!(num(discount) > 0)) discount.value = '10';
      syncOffer();
      preview();
    });
    productForm.querySelectorAll('[data-preview-input]').forEach((input) => input.addEventListener('input', preview));

    const open = (data) => {
      productForm.querySelectorAll('.mb-field__error, [data-conflict]').forEach((el) => el.remove());
      productForm.querySelectorAll('.mb-input--error').forEach((el) => el.classList.remove('mb-input--error'));
      const base = productForm.dataset.baseAction;
      productForm.action = data.id ? base + '/' + data.id : base;
      field('id').value = data.id || '';
      field('version').value = data.version || '';
      field('name').value = data.name || '';
      field('category').value = data.category || field('category').options[0].value;
      field('cost').value = data.cost || '';
      field('markupPercentage').value = data.markup || '';
      field('discountPercentage').value = data.discount || '0';
      field('stock').value = data.stock || '';
      field('description').value = data.description || '';
      offerToggle.checked = Number(data.discount || 0) > 0;
      document.getElementById('pm-title').textContent = data.id ? 'Editar produto' : 'Novo produto';
      deleteButton.hidden = !data.id;
      deleteButton.dataset.id = data.id || '';
      deleteButton.dataset.name = data.name || '';
      syncOffer();
      preview();
      productModal.showModal();
    };
    document.querySelectorAll('[data-product-new]').forEach((button) => button.addEventListener('click', () => open({})));
    document.querySelectorAll('[data-product-edit]').forEach((button) => button.addEventListener('click', () => open(button.dataset)));

    deleteButton.dataset.id = field('id').value;
    deleteButton.dataset.name = field('name').value;
    syncOffer();
    preview();
  }

  const zoneModal = document.getElementById('zone-modal');
  const zoneForm = zoneModal?.querySelector('[data-zone-form]');
  if (zoneModal && zoneForm) {
    const field = (name) => zoneForm.elements.namedItem(name);
    const deleteButton = zoneForm.querySelector('[data-modal-delete]');
    const open = (data) => {
      zoneForm.querySelectorAll('.mb-field__error, [data-zone-global-error]').forEach((el) => el.remove());
      zoneForm.querySelectorAll('.mb-input--error').forEach((el) => el.classList.remove('mb-input--error'));
      const base = zoneForm.dataset.baseAction;
      zoneForm.action = data.id ? base + '/' + data.id : base;
      field('id').value = data.id || '';
      field('name').value = data.name || '';
      field('cepStart').value = data.cepStart || '';
      field('cepEnd').value = data.cepEnd || '';
      field('fee').value = data.fee || '';
      field('deliveryDays').value = data.days || '';
      document.getElementById('zm-title').textContent = data.id ? 'Editar zona' : 'Nova zona';
      deleteButton.hidden = !data.id;
      deleteButton.dataset.id = data.id || '';
      deleteButton.dataset.name = data.name || '';
      zoneModal.showModal();
    };
    document.querySelectorAll('[data-zone-new]').forEach((button) => button.addEventListener('click', () => open({})));
    document.querySelectorAll('[data-zone-edit]').forEach((button) => button.addEventListener('click', () => open(button.dataset)));

    deleteButton.dataset.id = field('id').value;
    deleteButton.dataset.name = field('name').value;
  }

  const deleteModal = document.getElementById('delete-modal');
  const deleteForm = deleteModal?.querySelector('[data-delete-form]');
  if (deleteModal && deleteForm) {
    const confirmDelete = (id, name) => {
      deleteForm.action = deleteForm.dataset.baseAction + '/' + id + '/delete';
      deleteForm.querySelector('[data-delete-name]').textContent = name;
      productModal?.close();
      zoneModal?.close();
      deleteModal.showModal();
    };
    document.querySelectorAll('[data-product-delete], [data-zone-delete], [data-modal-delete]').forEach((button) => {
      button.addEventListener('click', () => confirmDelete(button.dataset.id, button.dataset.name));
    });
  }
});

document.addEventListener('DOMContentLoaded', () => {
  const status = document.querySelector('[data-copy-status]');
  document.querySelectorAll('[data-copy]').forEach((button) => {
    const label = button.getAttribute('aria-label');
    button.addEventListener('click', async () => {
      let message = 'Copiado!';
      try {
        await navigator.clipboard.writeText(button.dataset.copy);
      } catch (e) {
        message = 'Não deu para copiar; selecione o texto';
      }
      button.classList.add('is-copied');
      button.setAttribute('aria-label', message);
      button.title = message;
      if (status) status.textContent = message;
      setTimeout(() => {
        button.classList.remove('is-copied');
        button.setAttribute('aria-label', label);
        button.title = 'Copiar';
      }, 1500);
    });
  });
  document.querySelectorAll('[data-use-account]').forEach((button) => {
    button.addEventListener('click', () => {
      const email = document.querySelector('input[name="email"]');
      const password = document.querySelector('input[name="senha"]');
      if (!email || !password) return;
      email.value = button.dataset.email;
      password.value = button.dataset.password;
      password.focus();
    });
  });
});

document.addEventListener('DOMContentLoaded', () => {
  if (!window.matchMedia('(hover: hover)').matches) return;
  document.querySelectorAll('[data-cart-preview]').forEach((wrapper) => {
    const trigger = wrapper.querySelector('[aria-controls]');
    const panel = trigger && document.getElementById(trigger.getAttribute('aria-controls'));
    if (!panel) return;
    let closeTimer = null;
    let skipFocusOpen = false;
    const open = () => {
      clearTimeout(closeTimer);
      panel.hidden = false;
      trigger.setAttribute('aria-expanded', 'true');
    };
    const close = () => {
      clearTimeout(closeTimer);
      panel.hidden = true;
      trigger.setAttribute('aria-expanded', 'false');
    };
    const closeSoon = () => {
      clearTimeout(closeTimer);
      closeTimer = setTimeout(close, 250);
    };
    wrapper.addEventListener('pointerenter', (event) => {
      if (event.pointerType !== 'touch') open();
    });
    wrapper.addEventListener('pointerleave', (event) => {
      if (event.pointerType !== 'touch' && !wrapper.contains(document.activeElement)) closeSoon();
    });
    wrapper.addEventListener('focusin', () => {
      if (skipFocusOpen) {
        skipFocusOpen = false;
        return;
      }
      open();
    });
    wrapper.addEventListener('focusout', (event) => {
      if (!wrapper.contains(event.relatedTarget)) closeSoon();
    });
    document.addEventListener('keydown', (event) => {
      if (event.key !== 'Escape' || panel.hidden) return;
      close();
      if (wrapper.contains(document.activeElement) && document.activeElement !== trigger) {
        skipFocusOpen = true;
        trigger.focus();
      }
    });
  });
});
