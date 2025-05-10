// Main JavaScript file for the Media Processing Application

document.addEventListener('DOMContentLoaded', function() {
    console.log('Media Processing Application initialized');
    
    // Initialize tooltips
    const tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
    tooltipTriggerList.map(function (tooltipTriggerEl) {
        return new bootstrap.Tooltip(tooltipTriggerEl);
    });
    
    // Initialize popovers
    const popoverTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="popover"]'));
    popoverTriggerList.map(function (popoverTriggerEl) {
        return new bootstrap.Popover(popoverTriggerEl);
    });
    
    // Add event listeners for search functionality
    const searchInput = document.getElementById('searchInput');
    if (searchInput) {
        searchInput.addEventListener('input', function() {
            const searchTerm = this.value.toLowerCase();
            const mediaItems = document.querySelectorAll('#media-gallery .col-md-4');
            
            mediaItems.forEach(item => {
                const title = item.querySelector('.card-title').textContent.toLowerCase();
                const type = item.querySelector('.badge').textContent.toLowerCase();
                
                if (title.includes(searchTerm) || type.includes(searchTerm)) {
                    item.style.display = '';
                } else {
                    item.style.display = 'none';
                }
            });
        });
    }
    
    // Add event listeners for filter functionality
    const filterButtons = document.querySelectorAll('.filter-btn');
    if (filterButtons.length > 0) {
        filterButtons.forEach(button => {
            button.addEventListener('click', function() {
                const filterType = this.dataset.filter;
                const mediaItems = document.querySelectorAll('#media-gallery .col-md-4');
                
                // Update active button
                document.querySelectorAll('.filter-btn').forEach(btn => {
                    btn.classList.remove('active');
                });
                this.classList.add('active');
                
                // Filter items
                if (filterType === 'all') {
                    mediaItems.forEach(item => {
                        item.style.display = '';
                    });
                } else {
                    mediaItems.forEach(item => {
                        const type = item.querySelector('.badge').textContent.toLowerCase();
                        if (type === filterType.toLowerCase()) {
                            item.style.display = '';
                        } else {
                            item.style.display = 'none';
                        }
                    });
                }
            });
        });
    }
});
